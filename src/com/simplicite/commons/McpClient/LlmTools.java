package com.simplicite.commons.McpClient; // NOSONAR - name of module

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import com.simplicite.util.exceptions.DeleteException;
import com.simplicite.util.exceptions.ValidateException;
import com.simplicite.util.exceptions.SaveException;
import com.simplicite.util.exceptions.GetException;
import com.simplicite.util.exceptions.SearchException;

import com.simplicite.util.Tool;
import com.simplicite.util.AppLog;
import com.simplicite.util.Grant;
import com.simplicite.util.BusinessObject ;
import com.simplicite.util.ModuleDB;
import com.simplicite.util.ObjectDB;
import com.simplicite.util.Message;
import com.simplicite.util.SystemParameters;

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
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;

/**
 * Shared code LlmTools
 */
public class LlmTools implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private static final String AI_PING_ERROR = "AI_PING_ERROR";
    private static final String SYSPARAM_AI_API_PARAM = "AI_API_PARAM";
    private static final String SYSPARAM_AI_CHAT_BOT_NAME = "AI_CHAT_BOT_NAME";
    private static final String SYSPARAM_AI_API_KEY = "AI_API_KEY";
    private static final String SYSPARAM_AI_API_URL = "AI_API_URL";
    public static final String CLAUDE_LLM = "claude";
    private static final String HUGGINGFACE_LLM = "HUGGINGFACE";
    private static final String MISTRAL_LLM = "mistral";
    private static final String AUTH_PREFIX = "Bearer ";
    private static final String AUTH_PROPERTY = "Authorization";
    public static final String CONTENT_KEY = "content";
    public  static final String MESSAGE_KEY = "message";
    private static final String MESSAGES_KEY = "messages";
    public static final String USAGE_KEY = "usage";
    private static final String PROVIDER_KEY = "provider";
    public static final String CALL_ID_KEY = "tool_call_id";
    public static final String TOOL_CALLS_KEY = "tool_calls";
    public static final String TOOL_USE_KEY = "tool_use";
    private static final String API_KEY = "api_key";
    private static final String MODEL_KEY = "model";
    private static final String CLAUDE_WS_KEY ="anthropic-workspace-id";
    private static final String GPT_PROJECT_KEY = "OpenAI-Project";
    private static final String GPT_ORGA_KEY = "OpenAI-Organization";
    public static final String CHOICES_KEY = "choices";
    private static final String TOOL_CHOICE_KEY = "tool_choice";
    public static final String ERROR_KEY = "error";
    private static final String BOT_NAME_KEY = "bot_name";
    private static final String COMPLETION_KEY = "completion_url";
    private static final String PING_KEY = "ping_url";
    private static final String MAX_TOKEN_PARAM_KEY = "default_max_token";
    private static final String DEPTH_KEY = "hist_depth";
    public static final String ASSISTANT_ROLE = "assistant";
    private static final String SYSTEM_ROLE = "system";
    private static final String CALLER_PARAM_SPE = "specialisation";
    private static final String CALLER_PARAM_HISTORIC = "historic";
    private static final String CALLER_PARAM_SECURE = "secure";
    private static final String CALLER_PARAM_SAFE_SPE = "isSafeSpe";
    private static final String CALLER_PARAM_TOKEN = "maxToken";
    private static final String CALLER_PARAM_CODE_SECURE = "code_secure";
    private static final String MAX_TOKEN = "max_tokens";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE_URL = "image_url";
    private static final String TRUSTED = "trusted";
    private static final String SYS_CODE = "sys_code";
    private static final String SYS_VAL2 = "sys_value2";
    private static final String DEFAULT_MODULE = "System";
    private static final String ROW_MLD_ID = "row_module_id";
    public static final String PING_SUCCESS = "200";
    private static final String STT_URL_ERROR = "STT url not set";
    public static final String TOOL_FUNCTION_KEY = "function";
    private static JSONObject aiApiParam = getOptAiApiParam();
    private static final boolean IS_ENV_SETUP = !Tool.isEmpty(System.getenv(SYSPARAM_AI_API_PARAM))
            || !Tool.isEmpty(System.getenv("SIMPLICITE_SYSPARAM_AI_API_PARAM"));
    private static int aiHistDepth = aiApiParam.optInt(DEPTH_KEY);
    private static String aiChatBotName = getAIParam(BOT_NAME_KEY, "George");
    private static String llm = getLLMFromParams();
    private static String aiProvider = getProviderFromParams();
    private static String apiKey = getAIParam(API_KEY);
    private static String completionUrl = getAIParam(COMPLETION_KEY);
    private static boolean showDataDisclaimer = aiApiParam.optBoolean("showDataDisclaimer", true);
    private static final String HTML_LEFT_COLUMN_ID = "left_column";
    private static final String LABEL_KEY = "label";
    public static final String INFO = "MCP_INFO";
    public static final String DEBUG = "MCP_DEBUG";
    public static final String SYSPARAM_OBJ = "SystemParam";
    public static class AITypeException extends Exception {
        private static final long serialVersionUID = 1L;

        public AITypeException(String object, String classname, String needClass) {
            super("Invalid type for " + object + ": " + classname + " need " + needClass);
        }
    }

    public static class AICallerParams {
        private String specialisation;
        private JSONArray prompt;
        private JSONArray historic;
        private JSONObject providerParams;
        private JSONArray tools;
        private JSONArray assistantToolsCalls;
        private JSONArray userToolsResponse;
        private int maxToken = 1500;

        public AICallerParams(Object promptObject, JSONObject params) throws AITypeException {
            boolean secure = params.optBoolean(CALLER_PARAM_SECURE, false);
            boolean isSafeSpe = params.optBoolean(CALLER_PARAM_SAFE_SPE, false);
            boolean codeSecure = params.optBoolean(CALLER_PARAM_CODE_SECURE, false);
            setPrompt(promptObject, secure, codeSecure);
            setSpecialisation(params.optString(CALLER_PARAM_SPE), isSafeSpe);
            historic = params.optJSONArray(CALLER_PARAM_HISTORIC, new JSONArray());
            providerParams = params.optJSONObject("providerParams", new JSONObject());
            if (!Tool.isEmpty(aiApiParam)) {
                maxToken = aiApiParam.getInt(MAX_TOKEN_PARAM_KEY);
            }
            if (params.has(CALLER_PARAM_TOKEN)) {
                Object token = params.get(CALLER_PARAM_TOKEN);
                if (token instanceof Integer) {
                    maxToken = (int) token;
                } else if (token instanceof Boolean && !(boolean) token) {
                    maxToken = 0;
                }
            }
        }

        public void setTools(JSONArray tools) {
            this.tools = tools;
        }

        public void setAssistantToolsCalls(JSONArray assistantToolsCalls) {
            this.assistantToolsCalls = assistantToolsCalls;
        }

        public void setUserToolsResponse(JSONArray userToolsResponse) {
            this.userToolsResponse = userToolsResponse;
        }

        private void setSpecialisation(String spe, boolean isSafe) {
            spe = removeAcent(spe);
            if (!isSafe)
                spe = JSONObject.quote(normalize(spe, true));
            if ("\"\"".equals(spe))
                spe = "";
            specialisation = spe;
        }

        private void setPrompt(Object promptObject, boolean isSafe, boolean codeSecure) throws AITypeException {
            switch (promptObject) {
                case String str:
                    prompt = new JSONArray();
                    if (codeSecure) {
                        String strPrompt = normalizeCode(str);
                        prompt.put(getformatedContentByType(strPrompt, TYPE_TEXT, true));
                    } else {
                        String strPrompt = normalize(str, isSafe);
                        prompt.put(getformatedContentByType(strPrompt, TYPE_TEXT, false));
                    }
                    break;
                case JSONArray arr:
                    prompt = arr;
                    break;
                default:
                    AppLog.log(INFO, LlmTools.class, "setPrompt" ,"Prompt must be a String or a JSONArray");
                    throw new AITypeException("prompt", promptObject.getClass().getName(), "String or JSONArray");
            }
            prompt = parsedPrompts(prompt, isSafe);
        }

        /**
         * Function to format the call to chatAI API.
         * Need the api key parameter set up with your key.
         * Use the aiApiParam hist_depth parameter to limit the number of exchanges in
         * the historic (useful to limit the number off token of requests).
         * 
         * @param g Grant
         * @return If API return code is 200: API answer else: error return.
         */
        public JSONObject aiCall(Grant g) {
            return new JSONObject(aiCaller(g));
        }

        private String aiCaller(Grant g) {
            String model = getAIParam(MODEL_KEY);
            boolean isClaudeAPI = CLAUDE_LLM.equals(llm);
            if (Tool.isEmpty(completionUrl)) {
                AppLog.warning("completion url not set");
                return "";
            }
            try {
                URI url = new URI(completionUrl);
                HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                addSpecificHeaders(connection, apiKey);
                // format data
                JSONObject postData = new JSONObject();
                if (maxToken > 0)
                    postData.put(MAX_TOKEN, maxToken);
                if (!Tool.isEmpty(providerParams)) {
                    addProviderParamsToPost(postData);
                }
                if (!Tool.isEmpty(model))
                    postData.put(MODEL_KEY, model);
                JSONArray messages = new JSONArray();
                // format specialisation.
                if (!Tool.isEmpty(specialisation))
                    messages.put(new JSONObject().put("role", SYSTEM_ROLE).put(CONTENT_KEY, specialisation));
                // add historic (restrict to Param histDepth the number of messages )
                if (!Tool.isEmpty(historic)) {
                    messages.putAll(getCleanHistoric(historic));
                }

                JSONObject lastHist;
                if (!Tool.isEmpty(historic)) {
                    messages.putAll(getCleanHistoric(historic));
                    lastHist = historic.optJSONObject(historic.length() - 1);
                }else{
                    lastHist = null;
                }


                if(Tool.isEmpty(lastHist) || !"tool".equals(lastHist.optString("role", ""))){
                    messages.put(new JSONObject().put("role", "user").put(CONTENT_KEY, prompt));
                }
                if (!Tool.isEmpty(assistantToolsCalls) && !Tool.isEmpty(userToolsResponse)) {
                    messages.put(new JSONObject().put("role", ASSISTANT_ROLE).put(CONTENT_KEY, "").put(TOOL_CALLS_KEY,
                            assistantToolsCalls));
                    messages.putAll(userToolsResponse);
                }

                postData.put(MESSAGES_KEY, messages);
                if (!Tool.isEmpty(tools)) {
                    /**
                     * Add MCP TOOLS in call to AI API
                     */
                    postData.put("tools", tools);
                    postData.put(TOOL_CHOICE_KEY, "auto");
                }
                if (HUGGINGFACE_LLM.equals(llm)) {
                    postData = getHuggingFormatData(postData);
                }
                if (isClaudeAPI) {
                    postData = getClaudeFormatData(postData);
                }
                AppLog.log(DEBUG, LlmTools.class, "aiCaller", "Post data llm:" + postData.toString(1));
                try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                    byte[] bytes = postData.toString().getBytes(StandardCharsets.UTF_8);
                    outputStream.write(bytes);
                    outputStream.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    JSONObject error = readError(connection, responseCode, g);
                    return Tool.isEmpty(error) ? "" : error.toString(); // NOSONAR - Tool.isEmpty is not null safe
                }

                return readResponse(connection, g);
            } catch (IOException | URISyntaxException e) {
                AppLog.error(e, g);
            }
            return "";
        }

        private void addProviderParamsToPost(JSONObject postData) {
            for (String key : providerParams.keySet()) {
                Object data = providerParams.get(key);
                if (!Tool.isEmpty(data)) {
                    try {
                        float dataFloat = parseFloatParam(data);
                        postData.put(key, dataFloat);
                    } catch (AITypeException e) {
                        AppLog.warning(e.getMessage());
                    }
                }

            }
        }

        private static float parseFloatParam(Object param) throws AITypeException {
            switch (param) {
                case Number num:
                    return num.floatValue();
                case String str:
                    return Float.parseFloat(str);
                default:
                    throw new AITypeException("provider parameters", param.getClass().getName(), "number");
            }
        }

        private static JSONObject getClaudeFormatData(JSONObject postData) {
            JSONArray messages = postData.getJSONArray(MESSAGES_KEY);
            int toremove = -1;
            JSONArray convertedMessages = new JSONArray();
            for (int i = 0; i < messages.length(); i++) {
                JSONObject message = messages.getJSONObject(i);
                String role = message.optString("role");
                if (SYSTEM_ROLE.equals(role)) {
                    String content = message.optString(CONTENT_KEY);
                    postData.put(SYSTEM_ROLE, content);
                    toremove = i;
                    continue; // do not add to convertedMessages
                }

                if ("tool".equals(role)) {
                    // Convert OpenAI-style tool result -> Claude tool_result block wrapped in a
                    // user message
                    JSONObject toolResultBlock = new JSONObject();
                    toolResultBlock.put("type", "tool_result");
                    toolResultBlock.put("tool_use_id", message.optString(CALL_ID_KEY));
                    toolResultBlock.put(CONTENT_KEY, message.optString(CONTENT_KEY));
                    JSONObject userMsg = new JSONObject();
                    userMsg.put("role", "user");
                    userMsg.put(CONTENT_KEY, new JSONArray().put(toolResultBlock));
                    convertedMessages.put(userMsg);
                    continue;
                }

                if (ASSISTANT_ROLE.equals(role) && message.has(TOOL_CALLS_KEY)) {
                    // Convert OpenAI-style tool_calls -> Claude tool_use blocks
                    JSONArray contentBlocks = new JSONArray();
                    String textContent = message.optString(CONTENT_KEY, "");
                    if (!Tool.isEmpty(textContent)) {
                        JSONObject textBlock = new JSONObject();
                        textBlock.put("type", "text");
                        textBlock.put("text", textContent);
                        contentBlocks.put(textBlock);
                    }

                    JSONArray toolCalls = message.getJSONArray(TOOL_CALLS_KEY);
                    for (int j = 0; j < toolCalls.length(); j++) {
                        JSONObject tc = toolCalls.getJSONObject(j);
                        JSONObject function = tc.getJSONObject(TOOL_FUNCTION_KEY);
                        JSONObject toolUseBlock = new JSONObject();
                        toolUseBlock.put("type", TOOL_USE_KEY);
                        toolUseBlock.put("id", tc.getString("id"));
                        toolUseBlock.put("name", function.getString("name"));
                        toolUseBlock.put("input", new JSONObject(function.optString("arguments", "{}")));
                        contentBlocks.put(toolUseBlock);
                    }

                    JSONObject assistantMsg = new JSONObject();
                    assistantMsg.put("role", ASSISTANT_ROLE);
                    assistantMsg.put(CONTENT_KEY, contentBlocks);
                    convertedMessages.put(assistantMsg);
                    continue;
                }

                if ("user".equals(role) && !Tool.isEmpty(message.optString(CONTENT_KEY))) {
                    Object content = message.opt(CONTENT_KEY);
                    if (content instanceof JSONArray contentArray) {
                        for (int j = 0; j < contentArray.length(); j++) {
                            JSONObject contentJson = contentArray.getJSONObject(j);
                            if (TYPE_IMAGE_URL.equals(contentJson.optString("type"))) {
                                refactorImageForClaudeAPI(contentJson);
                            }
                        }
                    }
                }

                convertedMessages.put(message);
            }

            postData.put(MESSAGES_KEY, convertedMessages);
            if (toremove >= 0) {
                // no longer needed since we rebuilt the array, but harmless if messages is
                // still referenced elsewhere
            }

            if (!postData.has(MAX_TOKEN)) {
                postData.put(MAX_TOKEN, aiApiParam.getInt(MAX_TOKEN_PARAM_KEY));
            }
            postData.put(TOOL_CHOICE_KEY, new JSONObject().put("type", postData.optString(TOOL_CHOICE_KEY, "auto")));
            return postData;
        }

        private static void refactorImageForClaudeAPI(JSONObject contentJson) {
            contentJson.put("type", "image");
            String url = contentJson.optJSONObject(TYPE_IMAGE_URL).optString("url");
            contentJson.remove(TYPE_IMAGE_URL);
            JSONObject source = new JSONObject();
            String regexUrl = "data:([\\w\\/]*);(\\w*),(.*)";
            Pattern pattern = Pattern.compile(regexUrl);
            Matcher matcher = pattern.matcher(url);
            if (matcher.matches()) {
                source.put("type", matcher.group(2));
                source.put("media_type", matcher.group(1));
                source.put("data", matcher.group(3));
            }
            if (!Tool.isEmpty(source)) {
                contentJson.put("source", source);
            }

        }

        private static JSONObject getHuggingFormatData(JSONObject postData) {
            JSONObject newPostData = new JSONObject();
            StringBuilder dialogBuilder = new StringBuilder("");
            JSONObject params = new JSONObject();
            if (postData.has(MAX_TOKEN))
                params.put("max_length", postData.getInt(MAX_TOKEN));
            JSONArray messages = postData.getJSONArray(MESSAGES_KEY);
            for (int i = 0; i < messages.length(); i++) {
                JSONObject message = messages.getJSONObject(i);
                String role = message.optString("role", "user");
                String content = getContent(message.get(CONTENT_KEY));
                switch (role) {
                    case ASSISTANT_ROLE:
                        dialogBuilder.append("bot: ");
                        dialogBuilder.append(content);
                        dialogBuilder.append("\n");
                        break;
                    case SYSTEM_ROLE:
                        if (Tool.isEmpty(content) || "\"\"".equals(content))
                            break;
                        dialogBuilder.append("context: ");
                        dialogBuilder.append(content);
                        dialogBuilder.append("\n");
                        break;
                    default:
                        dialogBuilder.append(content);
                        dialogBuilder.append("\n");
                        break;
                }

            }
            newPostData.put("inputs", dialogBuilder.toString());
            newPostData.put("parameters", params);
            return newPostData;
        }

        private static String getContent(Object contentObj) {
            StringBuilder contentBuilder = new StringBuilder("");
            if (contentObj instanceof JSONArray arrayContent) {
                for (int j = 0; j < arrayContent.length(); j++) {
                    JSONObject contentJson = arrayContent.getJSONObject(j);
                    if (contentJson.has(TYPE_TEXT)) {
                        contentBuilder.append(contentJson.getString(TYPE_TEXT));
                        contentBuilder.append("\n");
                    }
                }
                return contentBuilder.toString();
            } else if (contentObj instanceof String strContent) {
                return strContent;
            }
            return "";
        }

        private static JSONArray getCleanHistoric(JSONArray historic) {
            int len = historic.length();
            if (len <= 0 || len < aiHistDepth * 2) {
                return historic;
            } else {
                // to redo for skip tools
                JSONArray newHistoric = new JSONArray();
                int nbUsrMsg = 0;
                int begin = -1;
                int index = len - 1;
                while (nbUsrMsg < aiHistDepth && index > 0) {
                    if ("user".equals(historic.getJSONObject(index).optString("role"))) {
                        nbUsrMsg += 1;
                        begin = index;
                    }
                    index -= 1;
                }
                if (begin >= 0) {
                    for (int i = begin; i < len; i++)
                        newHistoric.put(historic.getJSONObject(i));
                    return newHistoric;
                }
            }
            return historic;
        }

        private static JSONArray parsedPrompts(JSONArray prompts, boolean secure) {
            JSONArray newPrompts = new JSONArray();
            for (Object p : prompts) {
                if (p instanceof JSONObject contentJson) {
                    if (!contentJson.optBoolean(TRUSTED, false) && contentJson.has(TYPE_TEXT)) {
                        contentJson.put(TYPE_TEXT,
                                JSONObject.quote(normalize(contentJson.getString(TYPE_TEXT), secure)));
                    }
                    if (contentJson.has(TRUSTED)) {
                        contentJson.remove(TRUSTED);
                    }
                    newPrompts.put(contentJson);
                } else {
                    newPrompts.put(JSONObject.quote(normalize((String) p)));
                }
            }
            return newPrompts;
        }

        private static String normalize(String text) {
            text = removeAcent(text);
            text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "")
                    .replaceAll("[^\\w\\(\\),`{}.\\[\\]\"@\\/:-]", " ");
            text = replaceSymboleBySafeHTML(text);
            return text;
        }

        private static String normalize(String text, boolean secure) {
            text = removeAcent(text);
            return secure ? Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "")
                    .replaceAll("[^\\w\\(\\),`{}.\\[\\]\"@\\/:-]", " ") : normalize(text);
        }

        private static String normalizeCode(String text) {
            return removeAcent(text);
        }

        private static String removeAcent(String text) {
            text = text.replaceAll("(?u)[éèêë]", "e")
                    .replaceAll("(?u)[àâä]", "a")
                    .replaceAll("(?u)[îï]", "i")
                    .replaceAll("(?u)[ôö]", "o")
                    .replaceAll("(?u)[ùûü]", "u")
                    .replaceAll("(?u)æ", "ae")
                    .replaceAll("(?u)œ", "oe");
            text = Pattern.compile("(?u)ç", Pattern.CANON_EQ).matcher(text).replaceAll("c");
            text = Pattern.compile("(?u)ÿ", Pattern.CANON_EQ).matcher(text).replaceAll("y");
            return text;
        }

        private static String replaceSymboleBySafeHTML(String text) {
            text = text.replace("\\n", "<br>")
                    .replace("\\{", "&#123;")
                    .replace("\\}", "&#125;")
                    .replace("\\(", "&#40;")
                    .replace("\\)", "&#41;")
                    .replace("\\[", "&#91;")
                    .replace("\\]", "&#93;")
                    .replace("\\.", "&#46;")
                    .replace("\\,", "&#44;")
                    .replace("\\`", "&#96;")
                    .replace("\\\"", "&#34;")
                    .replace("\\@", "&#64;")
                    .replace("\\/", "&#47;")
                    .replace("\\-", "&#45;");
            return text;
        }

        private static JSONObject getformatedContentByType(String content, String type, boolean trusted) {
            return getformatedContentByType(content, type, trusted, null);
        }

        private static JSONObject getformatedContentByType(String content, String type, boolean trusted,
                String detail) {
            JSONObject res = new JSONObject();
            switch (type) {
                case TYPE_TEXT:
                    res.put("type", TYPE_TEXT);
                    res.put(type, content);
                    if (trusted)
                        res.put(TRUSTED, trusted);
                    break;
                case TYPE_IMAGE_URL:
                    res.put("type", TYPE_IMAGE_URL);
                    JSONObject image = new JSONObject().put("url", content);
                    if (!Tool.isEmpty(detail)) {
                        image.put("detail", detail);
                    }
                    res.put(type, image);
                    break;
                default:
                    return null;
            }
            return res;
        }
    }

    private static JSONObject getOptAiApiParam() {
        String env = System.getenv(SYSPARAM_AI_API_PARAM);
        if (Tool.isEmpty(env)) {
            return getOptAiApiParamByGrant();
        }
        return new JSONObject(env);
    }

    public static String getAIParam(String key) {
        return getAIParam(key, "");
    }

    public static String getAIParam(String key, String defaultValue) {
        return aiApiParam.optString(key, defaultValue);
    }
    public static String getLlm(){
        return llm;
    }
    private static String getLLMFromParams() {
        if (aiApiParam.optBoolean("ClaudeAPI", false))
            return CLAUDE_LLM;
        if (aiApiParam.optBoolean("HuggingAPI", false))
            return HUGGINGFACE_LLM;
        if (MISTRAL_LLM.equals(getAIParam(PROVIDER_KEY)))
            return MISTRAL_LLM;
        return "openai";
    }

    private static String getProviderFromParams() {
        String provider = getAIParam(PROVIDER_KEY);
        if (Tool.isEmpty(provider)) {
            String regex = "\\/\\/([\\w\\.]+)";
            Pattern pattern = Pattern.compile(regex);
            String url = getAIParam(COMPLETION_KEY);
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                provider = matcher.group(1);
            }
            if (!Tool.isEmpty(provider)) {
                setParameters(aiApiParam.put(PROVIDER_KEY, provider));
            } else {
                provider = Grant.getSystemAdmin().T("AI_DEFAULT_PROVIDER_NAME");
            }
        }
        return provider;
    }

    private static JSONObject getOptAiApiParamByGrant() {
        Grant g = Grant.getSystemAdmin();
        String paramStr = g.getParameter(SYSPARAM_AI_API_PARAM);
        if (!Tool.isEmpty(paramStr)) {
            JSONObject param = new JSONObject(paramStr);
            if (g.hasParameter(SYSPARAM_AI_CHAT_BOT_NAME) || g.hasParameter(SYSPARAM_AI_API_KEY)
                    || g.hasParameter(SYSPARAM_AI_API_URL)) {
                patchSysParamMerged(param);
            }
            return param;
        }
        JSONObject param = new JSONObject();
        setParameters(param);
        return param;
    }

     public static void setParameters(JSONObject setting) {
        AppLog.info("test setParameters");
        AppLog.info("setting: " + setting.toString(1));
        if (IS_ENV_SETUP) {
            return;
        }
        if (Tool.isEmpty(setting)) {
            setting = new JSONObject();
        }
        JSONObject rawParameters = getRawParameters();
        if((!setting.has(API_KEY) || Tool.isEmpty(setting.optString(API_KEY))) && rawParameters.has(API_KEY)){
            setting.put(API_KEY, rawParameters.optString(API_KEY));
        }
        if(rawParameters.has(CLAUDE_WS_KEY) && !setting.has(CLAUDE_WS_KEY) || Tool.isEmpty(setting.optString(CLAUDE_WS_KEY))){
            setting.put(CLAUDE_WS_KEY, rawParameters.optString(CLAUDE_WS_KEY));
        }
        if(rawParameters.has(GPT_PROJECT_KEY) && !setting.has(GPT_PROJECT_KEY) || Tool.isEmpty(setting.optString(GPT_PROJECT_KEY))){
            setting.put(GPT_PROJECT_KEY, rawParameters.optString(GPT_PROJECT_KEY));
        }
        if(rawParameters.has(GPT_ORGA_KEY) && !setting.has(GPT_ORGA_KEY) || Tool.isEmpty(setting.optString(GPT_ORGA_KEY))){
            setting.put(GPT_ORGA_KEY, rawParameters.optString(GPT_ORGA_KEY));
        }
        Grant g = Grant.getSystemAdmin();
        try (BusinessObject bo = g.getBusinessObject(SYSPARAM_OBJ)) {
            bo.getTmpObject()
                    .withAllAccess()
                    .forCreateOrUpdate(SYS_CODE, SYSPARAM_AI_API_PARAM)
                    .withValue(SYS_CODE, SYSPARAM_AI_API_PARAM)
                    .withValue("sys_value", "{}")
                    .withValue("sys_type", "PRV")
                    .withValue(ROW_MLD_ID, ModuleDB.getModuleId(DEFAULT_MODULE))
                    .withValue(SYS_VAL2, setting.toString(1))
                    .validateAndSave();
        } catch (ValidateException | SaveException | GetException e) {
            AppLog.error(e, g);
        }

        SystemParameters.clearCache();
        if (!Tool.isEmpty(setting))
            reloadAIParams();
    }

    /**
     * This method is used to patch the merged system parameters.
     * It checks if the old AI sysparams style exists and if so, it patches the new
     * AI sysparams.
     * If there are conflicting parameters, the new parameters are preserved.
     * 
     * @param None
     * @return None
     */
    private static boolean patchSysParamMerged(JSONObject param) {
        Grant g = Grant.getSystemAdmin();
        if (!Tool.isEmpty(param)) {
            // bot name
            checkOldSysParam(SYSPARAM_AI_CHAT_BOT_NAME, BOT_NAME_KEY, param, g);
            // api key
            checkOldSysParam(SYSPARAM_AI_API_KEY, API_KEY, param, g);
            // api completion url
            checkOldSysParam(SYSPARAM_AI_API_URL, COMPLETION_KEY, param, g);
            setParameters(param);
        }
        return true;
    }

    private static void reloadAIParams() {
        aiApiParam = getOptAiApiParam();
        aiHistDepth = aiApiParam.optInt(DEPTH_KEY);
        aiChatBotName = getAIParam(BOT_NAME_KEY, "George");
        llm = getLLMFromParams();
        completionUrl = getAIParam(COMPLETION_KEY);
        aiProvider = getAIParam(PROVIDER_KEY);
        apiKey = getAIParam(API_KEY);
    }

    private static Boolean checkOldSysParam(String name, String paramKey, JSONObject param, Grant g) {
        if (IS_ENV_SETUP) {
            return false;
        }
        if (g.hasParameter(name)) {
            String tmpVal = g.getParameter(name);
            if (!param.has(paramKey)) {
                param.put(paramKey, tmpVal);
            }
            try (BusinessObject bo = g.getBusinessObject(SYSPARAM_OBJ)) {
                bo.getTmpObject()
                        .withAllAccess()
                        .filter(SYS_CODE, name)
                        .searchAndDelete();
            } catch (DeleteException e) {
                AppLog.error(e, g);
                return false;
            }
        }
        return true;
    }

    public static String getProvider() {
        AppLog.log(DEBUG, LlmTools.class, "getProvider", aiProvider);
        return aiProvider;
    }

    public static String getBotName() {
        return aiChatBotName;
    }

    public static boolean checkSpeechRecognition() {
        if (Tool.isEmpty(getAIParam("stt_url"))) {
            AppLog.warning(STT_URL_ERROR);
            return false;
        }
        return true;
    }

    public static String pingAI() {
        Grant g = Grant.getSystemAdmin();
        String apiUrl = getAIParam(PING_KEY);
        if (Tool.isEmpty(apiUrl)) {
            AppLog.info("ping url not set", g);
            return Message.formatWarning("AI_NO_PING_URL", null, null);
        }
        try {
            URI url = new URI(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestMethod("GET");
            addSpecificHeaders(connection, apiKey);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return PING_SUCCESS;
            } else {
                JSONObject error = readError(connection, responseCode, g);
                if (Tool.isEmpty(error)) { 
                    return Message.formatError(AI_PING_ERROR, null, null);
                }
                return Message.formatError(AI_PING_ERROR, error.optString("code") + ": " + error.optString(ERROR_KEY), null);// NOSONAR - Tool.isEmpty check null

            }
        } catch (IOException | URISyntaxException e) {
            AppLog.error(e, g);
            return Message.formatError(AI_PING_ERROR, e.getMessage(), null);
        }

    }
    public static boolean isSetup(){
        return PING_SUCCESS.equals(pingAI());
    }

    /**
     * Reads the error response from an HTTP connection and returns a formatted
     * error message.
     *
     * @param connection   The HttpURLConnection object representing the connection.
     * @param responseCode The HTTP response code.
     * @param g            The Grant object.
     * @return A JSON-formatted error message containing the response code and error
     *         message.
     */
    private static JSONObject readError(HttpURLConnection connection, int responseCode, Grant g) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            JSONObject errorMessage = formatErrorMsg(responseCode, response);
            AppLog.log(INFO, LlmTools.class, "checkSpeechRecognition" ,"AI API error :[" + responseCode + "]: " + errorMessage.getString(ERROR_KEY));
            connection.disconnect();
            return errorMessage;
        } catch (IOException e) {
            AppLog.error(e, g);
        }
        connection.disconnect();
        return null;
    }

    /**
     * Reads the response from an HTTP connection and returns it as a string.
     *
     * @param connection the HTTP connection to read the response from
     * @param g          the Grant object for logging errors
     * @return the response from the HTTP connection as a string
     */
    public static String readResponse(HttpURLConnection connection, Grant g) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            connection.disconnect();
            String res = response.toString();
            JSONObject resJson = refactorAiResponseInGPT(res);
            if (resJson.has(USAGE_KEY)) {
                AppLog.log(INFO, LlmTools.class, "checkSpeechRecognition" ,"AI used token :" + logToken(resJson.optJSONObject(USAGE_KEY)));
            }
            return resJson.toString();
        } catch (IOException e) {
            AppLog.error(e, g);
        }
        connection.disconnect();
        return "";
    }

    private static String logToken(JSONObject json) {
        StringBuilder log = new StringBuilder();
        if (json.has("input_tokens")) {
            log.append("input tokens: ");
            log.append(String.valueOf(json.get("input_tokens")));
            log.append(", ");
            log.append("output tokens: ");
            log.append(String.valueOf(json.get("output_tokens")));
        } else if (json.has("prompt_tokens")) {
            log.append("input tokens: ");
            log.append(String.valueOf(json.get("prompt_tokens")));
            log.append(", ");
            log.append("output tokens: ");
            log.append(String.valueOf(json.get("completion_tokens")));
        }
        return log.toString();
    }

    private static void addSpecificHeaders(HttpURLConnection connection, String apiKey) {
        String projet = getAIParam(GPT_PROJECT_KEY);
        String org = getAIParam(GPT_ORGA_KEY);
        switch (llm) {
            case CLAUDE_LLM:
                connection.setRequestProperty("x-api-key", apiKey);
                connection.setRequestProperty("anthropic-version", "2023-06-01");
                connection.setRequestProperty(CLAUDE_WS_KEY, getAIParam(CLAUDE_WS_KEY));
                break;
            case HUGGINGFACE_LLM:
                connection.setRequestProperty(AUTH_PROPERTY, AUTH_PREFIX + apiKey);
                break;
            default:
                connection.setRequestProperty(AUTH_PROPERTY, AUTH_PREFIX + apiKey);
                if (!Tool.isEmpty(projet) && !Tool.isEmpty(org)) {
                    connection.setRequestProperty(GPT_PROJECT_KEY, projet);
                    connection.setRequestProperty(GPT_ORGA_KEY, org);
                }
                break;
        }
    }

    private static JSONObject formatErrorMsg(int responseCode, StringBuilder response) {
        String errorMessage;
        try {
            JSONObject error = new JSONObject(response.toString());
            errorMessage = error.optJSONObject(ERROR_KEY, new JSONObject()).optString(MESSAGE_KEY, response.toString());
        } catch (@SuppressWarnings("unused") JSONException e) {
            errorMessage = response.toString();
        }
        return new JSONObject().put("code", String.valueOf(responseCode)).put(ERROR_KEY, errorMessage);
    }

    public static JSONArray optJSONArray(String array) {
        try {
            return new JSONArray(array);
        } catch (@SuppressWarnings("unused") Exception e) {
            return new JSONArray();
        }
    }

    public static JSONObject aiCaller(Grant g, String specialisation, JSONArray historic, Object prompt) {
        try {
            JSONObject params = new JSONObject().put(CALLER_PARAM_SPE, specialisation)
                    .put(CALLER_PARAM_HISTORIC, historic);
            AICallerParams caller = new AICallerParams(prompt, params);
            return caller.aiCall(g);
        } catch (AITypeException e) {
            AppLog.error(e, g);
            return new JSONObject();
        }
    }

    public static JSONObject aiCallerWithMCP(Grant g, String specialisation, JSONArray historic, Object prompt,
            JSONArray tools, JSONArray assistantToolsCalls, JSONArray userToolsResponse) {
        try {
            JSONObject params = new JSONObject().put(CALLER_PARAM_SPE, specialisation).put(CALLER_PARAM_HISTORIC,
                    historic);
            AICallerParams caller = new AICallerParams(prompt, params);
            caller.setTools(tools);
            caller.setAssistantToolsCalls(assistantToolsCalls);
            caller.setUserToolsResponse(userToolsResponse);
            return caller.aiCall(g);
        } catch (AITypeException e) {
            AppLog.error(e, g);
            return new JSONObject();
        }
    }

    private static JSONObject refactorAiResponseInGPT(String res) {
        String resultText = "";
        switch (llm) {
            case "HUUGINGFACE":
                JSONArray resArray = optJSONArray(res);
                resultText = resArray.optJSONObject(0).getString("generated_text");
                return formatJsonOpenAIFormat(resultText);
            case CLAUDE_LLM:
                return claudeResponseToOpenAIFormat(new JSONObject(res));
            default:
                return new JSONObject(res);
        }
    }

    /**
     * Convert a native Claude (Anthropic) response to OpenAI-like format,
     * including tool_use blocks converted to tool_calls.
     */
    private static JSONObject claudeResponseToOpenAIFormat(JSONObject resJson) {
        JSONArray content = resJson.getJSONArray(CONTENT_KEY);
        StringBuilder text = new StringBuilder();
        JSONArray toolCalls = new JSONArray();
        for (int i = 0; i < content.length(); i++) {
            JSONObject block = content.getJSONObject(i);
            String type = block.optString("type");
            if ("text".equals(type)) {
                text.append(block.getString("text"));
            } else if (TOOL_USE_KEY.equals(type)) {
                JSONObject function = new JSONObject();
                function.put("name", block.getString("name"));
                function.put("arguments", block.getJSONObject("input").toString());
                JSONObject toolCall = new JSONObject();
                toolCall.put("id", block.getString("id"));
                toolCall.put("type", TOOL_FUNCTION_KEY);
                toolCall.put(TOOL_FUNCTION_KEY, function);
                toolCalls.put(toolCall);
            }
        }

        JSONObject message = new JSONObject();
        message.put("role", ASSISTANT_ROLE);
        message.put(CONTENT_KEY, text.isEmpty() ? text.toString() : JSONObject.NULL);
        if (toolCalls.length() > 0) {
            message.put(TOOL_CALLS_KEY, toolCalls);
        }

        JSONObject choice = new JSONObject();
        choice.put(MESSAGE_KEY, message);
        choice.put("finish_reason",
                TOOL_USE_KEY.equals(resJson.optString("stop_reason")) ? TOOL_CALLS_KEY : "stop");
        JSONObject gptFormat = new JSONObject();
        gptFormat.put(CHOICES_KEY, new JSONArray().put(choice));
        if (resJson.has(USAGE_KEY)) {
            gptFormat.put(USAGE_KEY, resJson.optJSONObject(USAGE_KEY));
        }
        return gptFormat;
    }

    public static JSONObject formatJsonOpenAIFormat(String result) {
        return new JSONObject().put(CHOICES_KEY,
                new JSONArray().put(new JSONObject().put(MESSAGE_KEY, new JSONObject().put(CONTENT_KEY, result))));
    }

    public static boolean isAIParam() {
        return isAIParam(true);
    }

    public static boolean isAIParam(boolean checkPing) {
        String ping = "";
        if (checkPing)
            ping = pingAI();
        return !(Tool.isEmpty(aiApiParam) || Tool.isEmpty(completionUrl)
                || (checkPing && (!"/".equals(aiApiParam.optString(PING_KEY, "/")) && !PING_SUCCESS.equals(ping))));
    }

    public static JSONObject getParameters(boolean forDisplay, String lang) {
        JSONObject params = new JSONObject(aiApiParam, JSONObject.getNames(aiApiParam));
        if (!forDisplay)
            return params;
        JSONObject defaultParam = new JSONObject(Grant.getSystemAdmin().T("AI_DEFAULT_PARAM"));
        JSONArray specificParam = new JSONArray();
        JSONObject newParam = new JSONObject();
        JSONArray leftColumn = new JSONArray();
        JSONArray rightColumn = new JSONArray();
        for (String key : params.keySet()) {
            if (API_KEY.equals(key))
                continue;
            if (defaultParam.has(key)) {
                JSONObject data = new JSONObject().put("field", key)
                        .put("value", getDisplayField(key, params.get(key), defaultParam.optJSONObject(key)))
                        .put(LABEL_KEY, optLabel(key, defaultParam, lang));
                if (defaultParam.optJSONObject(key).optBoolean(HTML_LEFT_COLUMN_ID)) {
                    leftColumn.put(data);
                } else {
                    rightColumn.put(data);
                }
            } else {
                specificParam.put(new JSONObject().put("key", key).put("value", checkPrivate(params, key)));
            }
        }
        newParam.put("columns", new JSONArray()
                .put(new JSONObject().put("class", HTML_LEFT_COLUMN_ID).put("fields", leftColumn))
                .put(new JSONObject().put("class", "right_column").put("fields", rightColumn)));
        newParam.put(PROVIDER_KEY, aiProvider);
        newParam.put("providerFields", specificParam);
        newParam.put("isConfigurable", isConfigurable());
        return newParam;
    }

    private static String getDisplayField(String key, Object value, JSONObject defaultField) {
        if (defaultField.optBoolean("private", false))
            return "********";
        switch (defaultField.optString("type")) {
            case "boolean":
                return (boolean) value ? "Yes" : "No";
            case "url":
                return "<a id=\"" + key + "\" href=\"" + value + "\">" + value + "</a>";
            default:
                return value.toString();
        }
    }

    public static boolean isConfigurable() {
        return !IS_ENV_SETUP;
    }

    private static Object checkPrivate(JSONObject params, String key) {
        if (!params.has(PROVIDER_KEY))
            return params.get(key);
        Grant g = Grant.getSystemAdmin();
        g.addResponsibility("MCP_CLT_ADMIN");
        ObjectDB obj = g.getTmpObject("AIProvider");
        synchronized (obj.getLock()) {
            obj.setFieldFilter("aiPrvProvider", params.getString(PROVIDER_KEY));
            List<String[]> res = obj.search();
            if (!res.isEmpty()) {
                JSONObject defParams = new JSONObject(res.get(0)[obj.getFieldIndex("aiPrvDataModel")]);
                if (defParams.has(key) && defParams.getJSONObject(key).optBoolean("private", false)) {
                    return "********";
                }
            }
        }
        return params.has(key) ? params.get(key) : null;
    }

    public static String optLabel(String key, JSONObject defaultFields, String lang) {
        JSONObject fieldDefLabel = defaultFields.optJSONObject(key, new JSONObject()).optJSONObject(LABEL_KEY);
        if (Tool.isEmpty(fieldDefLabel))
            return key.replaceAll("[_-]", " ");
        if (!fieldDefLabel.has(lang))
            lang = "ENU";
        String label = fieldDefLabel.optString(lang);
        return (Tool.isEmpty(fieldDefLabel) ? key.replaceAll("[_-]", " ") : label);
    }

    public static String getDataDisclaimer(Grant g) {
        if (showDataDisclaimer) {
            return g.T("AI_DISCLAIMER_DATA").replace("[PROVIDER]", aiProvider);
        }
        return "";
    }
     public static List<String> getModels(String url,String initialApiKey,Grant g) throws IOException, URISyntaxException{
        String regexenv = "^\\[ENV:(.*)\\]$";
        if(initialApiKey.matches(regexenv)){
            initialApiKey = System.getenv(initialApiKey.replaceAll(regexenv, "$1"));
        }
        if(Tool.isEmpty(initialApiKey) && !Tool.isEmpty(apiKey)){
            initialApiKey = apiKey;

        }
        URI apiUrl = new URI(url);
        HttpURLConnection connection = (HttpURLConnection) apiUrl.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty(AUTH_PROPERTY, AUTH_PREFIX + initialApiKey);
        ArrayList<String> res = new ArrayList<>();
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JSONObject resJson = new JSONObject(response.toString());
            for(Object model : resJson.optJSONArray("data")){
                JSONObject jsonModel = (JSONObject)model;
                if(jsonModel.has("id") && MODEL_KEY.equals(jsonModel.optString("object"))){
                    res.add(jsonModel.getString("id"));
                }
            }

        } else {
            JSONObject error = readError(connection,responseCode,g);
            res.add(ERROR_KEY);
            if(Tool.isEmpty(error)) error = new JSONObject();
            res.add(error.optString("code"));// NOSONAR - Tool.isEmpty check null
            res.add(error.optString(ERROR_KEY));

        }
        connection.disconnect();
        return res;
    }
     public static JSONObject getCurrentSetup(){
        JSONObject params = getOptAiApiParam();
        JSONObject data = new JSONObject();
        data.put("llmPrvCompletionUrl", params.optString(COMPLETION_KEY));
        data.put("llmPrvProvider", params.optString(PROVIDER_KEY));
        data.put("llmMaxToken", params.optString(MAX_TOKEN_PARAM_KEY));
        data.put("llmDataDisclamer", params.optBoolean("showDataDisclaimer"));
        data.put("llmHistoryDepth", params.optString(DEPTH_KEY));
        data.put("llmModel", params.optString(MODEL_KEY));
        data.put("llmPrvModelsUrl", params.optString("model_url", ""));
        data.put("llmPrvSttUrl", params.optString("stt_url", ""));
        data.put("llmPrvPingUrl", params.optString(PING_KEY, ""));
        return data;
    }
    private static JSONObject getRawParameters(){
        JSONObject result = new JSONObject();
        Grant g = Grant.getSystemAdmin();
        try (BusinessObject bo = g.getBusinessObject(SYSPARAM_OBJ)) {
            bo.getTmpObject()
                    .withAllAccess()
                    .filter(SYS_CODE, SYSPARAM_AI_API_PARAM)
                    .search((index, count, obj) -> {
                        String sysValue = obj.getFieldValue(SYS_VAL2);
                        if(Tool.isEmpty(sysValue)){
                            sysValue = obj.getFieldValue("sys_value");
                        }
                        JSONObject param = Tool.isEmpty(sysValue) ? new JSONObject() : new JSONObject(sysValue);
                        for(String key : param.keySet()){
                            result.put(key, param.getString(key));
                        }

                        return false;
                    });
        }catch(SearchException e){
            AppLog.error(e, g);
            return new JSONObject();
        }
        return result;
    }
}