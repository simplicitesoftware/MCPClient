package com.simplicite.commons.McpClient;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.*;

import com.simplicite.util.*;
import com.simplicite.util.exceptions.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.simplicite.util.tools.*;
import java.nio.charset.StandardCharsets;
/**

/**
 * Shared code LlmTools
 */
public class LlmTools implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final  boolean AI_DEBUG_LOGS ="true".equals(Grant.getSystemAdmin().getParameter("AI_DEBUG_LOGS"));
    private static final String AI_PING_ERROR="AI_PING_ERROR";
    private static final String SYSPARAM_AI_API_PARAM="AI_API_PARAM";
    private static final String SYSPARAM_AI_CHAT_BOT_NAME="AI_CHAT_BOT_NAME";
    private static final String SYSPARAM_AI_API_KEY="AI_API_KEY";
    private static final String SYSPARAM_AI_API_URL="AI_API_URL";
    private static final String CLAUDE_LLM ="CLAUDE";
    private static final String HUGGINGFACE_LLM ="HUGGINGFACE";
    private static final String MISTRAL_LLM ="Mistral AI_";
    private static final String AUTH_PREFIX = "Bearer ";
    private static final String AUTH_PROPERTY = "Authorization";

    public static final String CONTENT_KEY = "content";
    private static final String MESSAGE_KEY = "message";
    private static final String MESSAGES_KEY = "messages";
    public static final String USAGE_KEY = "usage";
    private static final String PROVIDER_KEY = "provider";
    private static final String API_KEY = "api_key";
    private static final String MODEL_KEY = "model";
    public static final String ERROR_KEY = "error";
    private static final String LABEL_KEY = "label";
    private static final String BOT_NAME_KEY = "bot_name";
    private static final String COMPLETION_KEY = "completion_url";

    private static final String MAX_TOKEN_PARAM_KEY = "default_max_token";
    private static final String ASSISTANT_ROLE="assistant";
    private static final String SYSTEM_ROLE= "system";
    private static final String HTML_LEFT_COLUMN_ID = "left_column";
    private static final String CALLER_PARAM_SPE ="specialisation";
    private static final String CALLER_PARAM_HISTORIC = "historic";
    private static final String CALLER_PARAM_SECURE = "secure";
    private static final String CALLER_PARAM_SAFE_SPE ="isSafeSpe";
    private static final String CALLER_PARAM_TOKEN ="maxToken";
    private static final String CALLER_PARAM_CODE_SECURE = "code_secure";
    private static final String MAX_TOKEN = "max_tokens";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE_URL = "image_url";

    private static final String TRUSTED = "trusted";
    private static final String SWAGGER_COMPONENTS="components";
    private static final String SWAGGER_SHEMAS="schemas";

    private static final String SYS_CODE = "sys_code";
    private static final String SYS_VAL2 = "sys_value2";
    private static final String DEFAULT_MODULE = "System";
    private static final String ROW_MLD_ID = "row_module_id";

    public static final String PING_SUCCESS = "200";
    private static final String STT_URL_ERROR = "STT url not set";

    private static  JSONObject aiApiParam =getOptAiApiParam();
    private static final boolean IS_ENV_SETUP =  !Tool.isEmpty(System.getenv(SYSPARAM_AI_API_PARAM)) ||!Tool.isEmpty(System.getenv("SIMPLICITE_SYSPARAM_AI_API_PARAM"));
    private static  int aiHistDepth = aiApiParam.optInt("hist_depth");
    private static  String aiChatBotName = getAIParam(BOT_NAME_KEY, "George");
    private static  String llm = getLLM();
    private static  boolean showDataDisclaimer = aiApiParam.optBoolean("showDataDisclaimer",true);
    private static  String aiProvider = getProvider();
    private static String apiKey = getAIParam(API_KEY);
    private static String completionUrl = getAIParam(COMPLETION_KEY);
    public static class AITypeException extends Exception {
        private static final long serialVersionUID = 1L;

        public AITypeException(String object, String classname, String needClass) {
            super("Invalid type for "+object+": "+classname+" need "+needClass);
        }
    }
    private static JSONObject getOptAiApiParam(){
        String env = System.getenv(SYSPARAM_AI_API_PARAM);
        if(Tool.isEmpty(env)){
            return getOptAiApiParamByGrant();
        }
        //importDatasets(ModuleDB.getModuleId("AIBySimplicite"));
        return new JSONObject(env);
    }
    public static String getAIParam(String key){
        return getAIParam(key,"");
    }
    public static String getAIParam(String key,String defaultValue){
        return aiApiParam.optString(key,defaultValue);
    }
    private static String getLLM(){
        if(aiApiParam.optBoolean("ClaudeAPI", false)) return CLAUDE_LLM;
        if(aiApiParam.optBoolean("HuggingAPI", false)) return HUGGINGFACE_LLM;
        if(MISTRAL_LLM.equals(getAIParam(PROVIDER_KEY))) return MISTRAL_LLM;
        return "GPT";
    }
    private static String getProvider(){
        String provider = getAIParam(PROVIDER_KEY);
        if(Tool.isEmpty(provider)){
            String regex = "\\/\\/([\\w\\.]+)";
            Pattern pattern = Pattern.compile(regex);
            String url = getAIParam(COMPLETION_KEY);
            Matcher matcher = pattern.matcher(url);
            if(matcher.find()){
                provider = matcher.group(1);
            }
            if(!Tool.isEmpty(provider)){
                setParameters(aiApiParam.put(PROVIDER_KEY, provider));   
            }else{
                provider = Grant.getSystemAdmin().T("AI_DEFAULT_PROVIDER_NAME");
            }
        }
        return provider;
    }
    private static JSONObject getOptAiApiParamByGrant(){
        Grant g = Grant.getSystemAdmin();
        String paramStr = g.getParameter(SYSPARAM_AI_API_PARAM);
        if (!Tool.isEmpty(paramStr)) {
            JSONObject param = new JSONObject(paramStr);
            if(g.hasParameter(SYSPARAM_AI_CHAT_BOT_NAME) || g.hasParameter(SYSPARAM_AI_API_KEY) || g.hasParameter(SYSPARAM_AI_API_URL)){
                patchSysParamMerged(param);
            }
            return param;
        }
        JSONObject param = new JSONObject();
        setParameters(param);
        return param;
    }
    public static void setParameters(JSONObject setting){
        if (IS_ENV_SETUP){
            return;
        }
        Grant g = Grant.getSystemAdmin();
        ObjectDB paramObj = g.getTmpObject("SystemParam");
        BusinessObjectTool paramTool = paramObj.getTool();
        synchronized(paramObj.getLock()){
            try {
                if(!paramTool.selectForUpsert(new JSONObject().put(SYS_CODE, SYSPARAM_AI_API_PARAM))){
                    paramObj.setFieldValue(SYS_CODE, SYSPARAM_AI_API_PARAM);
                    paramObj.setFieldValue("sys_value", "{}");
                    paramObj.setFieldValue("sys_type", "PRV");
                    paramObj.setFieldValue(ROW_MLD_ID,ModuleDB.getModuleId(DEFAULT_MODULE));
                }
                if(!Tool.isEmpty(setting)) paramObj.setFieldValue(SYS_VAL2, setting.toString(1));
                paramTool.validateAndSave();
            } catch (GetException | JSONException | ValidateException | SaveException e) {
                AppLog.error( e, g);

            }
        }
        SystemParameters.clearCache();
        if(!Tool.isEmpty(setting)) reloadAIParams();
    }
    /**
     * This method is used to patch the merged system parameters.
     * It checks if the old AI sysparams style exists and if so, it patches the new AI sysparams.
     * If there are conflicting parameters, the new parameters are preserved.
     * 
     * @param None
     * @return None
     */
    private static boolean patchSysParamMerged(JSONObject param){
        Grant g = Grant.getSystemAdmin();
        if(!Tool.isEmpty(param)){
            //bot name
            checkOldSysParam(SYSPARAM_AI_CHAT_BOT_NAME,BOT_NAME_KEY,param,g);

            //api key
            checkOldSysParam(SYSPARAM_AI_API_KEY, API_KEY, param, g);
            //api completion url
            checkOldSysParam(SYSPARAM_AI_API_URL, COMPLETION_KEY, param, g);

            setParameters(param);
        }
        return true;
    }
    
    private static void reloadAIParams(){
                aiApiParam =getOptAiApiParam();
        aiHistDepth = aiApiParam.optInt("hist_depth");
        aiChatBotName = getAIParam(BOT_NAME_KEY, "George");
        llm = getLLM();
        completionUrl = getAIParam(COMPLETION_KEY);
        showDataDisclaimer = aiApiParam.optBoolean("showDataDisclaimer",true);
        aiProvider = getAIParam(PROVIDER_KEY);
        apiKey = getAIParam(API_KEY);
    }
    private static Boolean checkOldSysParam(String name,String paramKey,JSONObject param,Grant g){
        if(IS_ENV_SETUP){
            return false;
        }
        if(g.hasParameter(name)){
            String tmpVal= g.getParameter(name);
            if(!param.has(paramKey)){
                param.put(paramKey,tmpVal);
            }
            ObjectDB paramObj = g.getTmpObject("SystemParam");
            BusinessObjectTool paramTool = paramObj.getTool();
            synchronized(paramObj.getLock()){
                try{
                    List<String[]> parameters = paramTool.search(new JSONObject().put(SYS_CODE,name));
                    if(parameters.size()==1){
                        paramTool.selectForDelete(parameters.get(0)[paramObj.getRowIdFieldIndex()]);
                        paramTool.delete();

                    }
                }catch(GetException | DeleteException | SearchException | JSONException e){
                    AppLog.error(e,g);
                    return false;
                }

            }
        }
        return true;

    }

}
