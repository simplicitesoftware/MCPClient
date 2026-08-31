package com.simplicite.extobjects.McpClient;

import java.util.*;

import org.json.*;

import com.simplicite.util.*;
import com.simplicite.util.exceptions.*;
import com.simplicite.util.tools.*;
import com.simplicite.commons.AIBySimplicite.AITools;
import com.simplicite.commons.McpClient.McpClientManager;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Content;

/** REST service external object AiMcpClientApi */
public class AiMcpClientApi extends com.simplicite.webapp.services.RESTServiceExternalObject {
    private static final long serialVersionUID = 1L;
    private static McpClientManager
            manager; // = McpClientManager.getInstance(Grant.getSystemAdmin());
    private static JSONArray tools; // = manager.listToolsAsOpenAIFormat();
    private static String serverInstructions;

    private static final String JSON_OBJECT_NAME_KEY = "objectName";
    private static final String JSON_OBJECT_ID_KEY = "objectID";
    private static final String PARAMS_PROMPT_KEY = "prompt";
    private static final String JSON_REQ_TYPE = "reqType";

    @Override
    public void init(Parameters params) {
        Grant g = getGrant();
        if (AITools.AI_DEBUG_LOGS) AppLog.info("init API with GRANT " + g.getLogin());
        manager = McpClientManager.getInstance(g);
        tools = manager.listToolsAsOpenAIFormat();
        serverInstructions = manager.getServerInstructions();
    }

    /** GET : liste les outils MCP disponibles */
    @Override
    public Object get(Parameters params) throws HTTPException {
        try {
            return super.get(params);
        } catch (Exception e) {
            AppLog.error("McpClientApi GET error: " + e.getMessage(), e, getGrant());
            return error(e);
        }
    }

    /**
     * POST : envoie un prompt, retourne réponse ou demande d'action Body attendu : { "prompt":
     * "...", "tool": "..." (optionnel) }
     */
    @Override
    public Object post(Parameters params) throws HTTPException {

        if (AITools.AI_DEBUG_LOGS) {
            AppLog.info("McpClientApi POST tools: " + tools.toString(1), getGrant());
        }
        try {
            JSONObject req = params.getJSONObject();
            String prompt = getParamOrreqParam(PARAMS_PROMPT_KEY, params, req);
            String objectName = getParamOrreqParam(JSON_OBJECT_NAME_KEY, params, req);
            String type = getParamOrreqParam(JSON_REQ_TYPE, params, req);
            String objectID = getParamOrreqParam(JSON_OBJECT_ID_KEY, params, req);
            if (Tool.isEmpty(type)) type = "default";
            else if (!Tool.isEmpty(objectName) && !Tool.isEmpty(objectID))
                type = Tool.isEmpty(prompt) ? "frontAiCall" : "paramField";

            switch (type) { // use switch for future extension
                case "provider":
                    return new JSONObject().put("provider", AITools.provider());
                case "chatBot":
                    return chatbotCaller(prompt, params, req);
                case "metrics":
                    return badRequest("Metrics are not supported yet with MCP");
                case "saveMetrics":
                    return badRequest("Metrics are not supported yet with MCP");
                case "errorMetricsSolver":
                    return badRequest("Metrics are not supported yet with MCP");
                case "reformulateMetrics":
                    return badRequest("Metrics are not supported yet with MCP");
                case "BOT_NAME":
                    return new JSONObject().put("botName", AITools.getBotName());
                case "CHECK_SPEECH_RECOGNITION":
                    return new JSONObject()
                            .put("isSpeechRecognitionSupported", AITools.checkSpeechRecognition());
                case "ping":
                    return ping();
                case "audio":
                    return badRequest("Audio are not supported yet with MCP");
                case "requestField":
                    return badRequest("Request field will not be supported with MCP");
                case "paramField":
                    return badRequest("Param field will not be supported with MCP");
                case "frontAiCall":
                    return badRequest("Front AI call will not be supported with MCP");
                case "commentCode":
                    return badRequest("Comment code is not supported yet with MCP");
                case "recallWithTools":
                    return recallWithTools(prompt, params, req);
                default:
                    AppLog.info("AI API ERROR: " + type + params.toJSON());
                    return error(
                            400,
                            "Call me with a predefined request type, prompt or a object param please!");
            }

        } catch (Exception e) {
            AppLog.error(null, e, getGrant());
            return error(e);
        }
    }

    private Object recallWithTools(String prompt, Parameters params, JSONObject req) {
        try {
            JSONArray acceptedTools = req.optJSONArray("acceptedTools", new JSONArray());
            JSONArray refusedTools = req.optJSONArray("refusedTools", new JSONArray());
            return chatbotCaller(prompt, params, req, acceptedTools, refusedTools);

        } catch (Exception e) {
            AppLog.error("McpClientApi recallWithTools error: " + e.getMessage(), e, getGrant());
            return error(e);
        }
    }

    private Object ping() {
        String ping = AITools.pingAI();
        boolean isSuccess = AITools.PING_SUCCESS.equals(ping);
        if (isSuccess) {
            ping = Message.formatInfo("AI_SUCCESS_PING", null, null);
        }
        return new JSONObject().put("msg", ping);
    }

    private Object chatbotCaller(String prompt, Parameters params, JSONObject req) {
        return chatbotCaller(prompt, params, req, null, null);
    }

    private Object chatbotCaller(
            String prompt,
            Parameters params,
            JSONObject req,
            JSONArray acceptedTools,
            JSONArray refusedTools) {
        boolean istool = false;
        try {
            prompt = prompt.trim();
            if (Tool.isEmpty(prompt)) {
                return badRequest("need a prompt");
            }
            if (AITools.AI_DEBUG_LOGS) {
                AppLog.info("McpClientApi POST request: " + req.toString(1), getGrant());
            }
            String id = getParamOrreqParam("id",params,req);
            JSONArray historic = Tool.isEmpty(id)?AITools.optJSONArray(getParamOrreqParam("historic", params, req)):getHistoric(id);
            if (tools.length() == 0) {
                AppLog.warning("NO MCP TOOLS FOUND, Call api without tools");
                JSONObject result = AITools.aiCaller(getGrant(), serverInstructions, historic, prompt);
                addHist(id,result,prompt,null,null);
                return result;
            } else {

                JSONArray assistantToolsCalls = new JSONArray();
                JSONArray userToolsResponse = new JSONArray();
                JSONArray p = AITools.optJSONArray(prompt);
                if (acceptedTools != null && acceptedTools.length() > 0
                        || refusedTools != null && refusedTools.length() > 0) {
                   // istool = true;

                    for (int i = 0; i < acceptedTools.length(); i++) {
                        JSONObject tool = acceptedTools.getJSONObject(i);
                        if (tool.has("description")) {
                            tool.remove("description");
                        }
                        if (tool.has("index")) {
                            tool.remove("index");
                        }
                        tool.remove("index");
                        assistantToolsCalls.put(tool);
                        JSONObject userToolResponse = new JSONObject();
                        userToolResponse.put("tool_call_id", tool.optString("id"));
                        userToolResponse.put("role", "tool");
                        userToolResponse.put(AITools.CONTENT_KEY, getToolResponse(tool));
                        userToolsResponse.put(userToolResponse);
                    }
                    for (int i = 0; i < refusedTools.length(); i++) {

                        JSONObject tool = refusedTools.getJSONObject(i);
                        if (tool.has("description")) {
                            tool.remove("description");
                        }
                        if (tool.has("index")) {
                            tool.remove("index");
                        }
                        tool.remove("index");
                        assistantToolsCalls.put(tool);
                        JSONObject userToolResponse = new JSONObject();
                        userToolResponse.put("tool_call_id", tool.optString("id"));
                        userToolResponse.put("role", "tool");
                        userToolResponse.put(AITools.CONTENT_KEY, "Tool execution denied by user. Ask the user what they would like to do now");
                        userToolsResponse.put(userToolResponse);
                    }
                }
                if(istool ){
                    int index = historic.length() -1;
                    if("assistant".equals(historic.getJSONObject(index).getString("role"))) historic.remove(index);

                }
                JSONObject response =
                        AITools.aiCallerWithMCP(
                                getGrant(),
                                serverInstructions,
                                historic,
                                Tool.isEmpty(p) ? prompt : p,
                                tools,
                                assistantToolsCalls,
                                userToolsResponse);
                if (AITools.AI_DEBUG_LOGS) {
                    AppLog.info("McpClientApi POST response: " + response.toString(1), getGrant());
                }
                JSONArray toolCall = new JSONArray();
                if ("tool_calls"
                        .equals(
                                response.optJSONArray("choices", new JSONArray())
                                        .optJSONObject(0, new JSONObject())
                                        .optString("finish_reason", ""))) {

                    toolCall =
                            response.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .optJSONObject("message", new JSONObject())
                                    .optJSONArray("tool_calls", new JSONArray());
                    for (int i = 0; i < toolCall.length(); i++) {
                        JSONObject tool = toolCall.getJSONObject(i);
                        String toolName =
                                tool.optJSONObject("function", new JSONObject())
                                        .optString("name", "");
                        String toolDescription = manager.getToolDescription(toolName);
                        tool.put("description", toolDescription);
                    }
                }

                JSONObject result = new JSONObject()
                        .put("tools", toolCall)
                        .put("request", prompt)
                        .put("response", response);
                addHist(id,result,Tool.isEmpty(p) ? prompt : p,userToolsResponse,assistantToolsCalls);
                return result;
            }
        } catch (Exception e) {
            AppLog.error("McpClientApi POST error: " + e.getMessage(), e, getGrant());
            return error(e);
        }
    }

    private void addHist(String id, JSONObject response, Object prompt,JSONArray toolsRep, JSONArray toolsCall){
        StringBuilder logs = new StringBuilder();
        logs.append("-------------DEBUG add Hist---------------\n");
        boolean isToolCall ="tool_calls".equals(response.optJSONObject("response",new JSONObject()).optJSONArray("choices", new JSONArray()).optJSONObject(0, new JSONObject()).optString("finish_reason", ""));
        logs.append("isToolCall: "+isToolCall+"\n");
        Grant g = getGrant();
        JSONObject json = g.getJSONObjectParameter("AI_CHAT_HIST","{}");
        if(!json.has(id))json.put(id,new JSONArray());
        JSONArray hist = json.getJSONArray(id);
        String usermsg = "not found";

        if(prompt instanceof  String){
            usermsg=(String)prompt;
        }else if(prompt instanceof  JSONArray arr){
            for(Object o: arr){
                JSONObject j = (JSONObject)o;
                if("text".equals(j.optString("type",""))){
                    usermsg=j.optString("text","not found");
                     break; 
                }
            }
        }
        usermsg = usermsg.replaceAll("^\"|\"$", "");
        logs.append("on user message: "+usermsg+"\n");
        if(isToolCall || !Tool.isEmpty(toolsCall)){
            logs.append("isToolCall check last Hist\n");

            JSONObject lastHist = hist.getJSONObject(hist.length()-1);
             logs.append(lastHist.toString(1)+"\n");
            if(!"tool".equals(lastHist.optString("role","")) && !("user".equals(lastHist.optString("role","")) && usermsg.equals(lastHist.optString(AITools.CONTENT_KEY,""))))hist.put(new JSONObject().put("role","user").put(AITools.CONTENT_KEY,usermsg));

        }else {
            logs.append("is not ToolCall append usrmsg\n");
            hist.put(new JSONObject().put("role","user").put(AITools.CONTENT_KEY,usermsg));

        }
        if(!Tool.isEmpty(toolsCall) && !Tool.isEmpty(toolsRep)){
            JSONObject calls = new JSONObject().put("role","assistant").put("tool_calls",toolsCall).put(AITools.CONTENT_KEY,JSONObject.NULL);
            hist.put(calls);
            for (Object o:toolsRep) {
                hist.put(o);  
            }
        }
        if(!isToolCall){ 
            String botResponse = response.optJSONObject("response",new JSONObject()).optJSONArray("choices",new JSONArray()).optJSONObject(0,new JSONObject()).optJSONObject("message",new JSONObject()).optString(AITools.CONTENT_KEY,"not found");
            hist.put(new JSONObject().put("role","assistant").put(AITools.CONTENT_KEY,Tool.isEmpty(botResponse)?"not found":botResponse));
       }
        g.setParameter("AI_CHAT_HIST",json.toString(1));
        g.setUserSystemParam("AI_CHAT_HIST",json.toString(1),false);
        logs.append("-------------END DEBUG add Hist---------------\n");
        AppLog.info("\n"+logs.toString());

    }


    private String getToolResponse(JSONObject tool) {
        JSONObject toolfunc = tool.optJSONObject("function", new JSONObject());
        String toolName = toolfunc.optString("name", "");
        JSONObject parametersJson;
        Object parameters = toolfunc.opt("arguments");
        if (parameters instanceof JSONObject) {
            parametersJson = (JSONObject) parameters;
        } else if (!Tool.isEmpty(parameters)) {
            parametersJson = new JSONObject(parameters.toString());
        } else {
            parametersJson = new JSONObject();
        }
        CallToolResult response = manager.callWithPrompt(toolName, parametersJson);

        if (Boolean.TRUE.equals(response.isError())) {
            StringBuilder errSb = new StringBuilder("Tool error: ");
            for (io.modelcontextprotocol.spec.McpSchema.Content content : response.content()) {
                if (content instanceof io.modelcontextprotocol.spec.McpSchema.TextContent tc) {
                    errSb.append(tc.text()).append("\n");
                } else {
                    errSb.append(content.toString()).append("\n");
                }
            }
            String errorMsg = errSb.toString().trim();
            AppLog.warning("Tool [" + toolName + "] error: " + errorMsg, null);
            return errorMsg;
        }
        StringBuilder sb = new StringBuilder();
        for (io.modelcontextprotocol.spec.McpSchema.Content content : response.content()) {
            // Extraire le texte selon le type
            if (content instanceof io.modelcontextprotocol.spec.McpSchema.TextContent tc) {
                sb.append(tc.text()).append("\n");
            } else {
                sb.append(content.toString()).append("\n");
            }
        }

        return sb.toString().trim();
    }
    private JSONArray getHistoric(String id){
        JSONObject json = getGrant().getJSONObjectParameter("AI_CHAT_HIST","{}");
        if(json.has(id)) return json.optJSONArray(id);
        return new JSONArray();
    }
    private String getParamOrreqParam(String name, Parameters params, JSONObject req) {

        if (Tool.isEmpty(name)) return null;
        String p = params.getParameter(name);
        if (Tool.isEmpty(p) && req.has(name)) {
            Object op = req.get(name);
            if (op instanceof JSONArray arr) {

                p = arr.toString();
            }
            if (op instanceof JSONObject obj) {
                p = obj.toString();
            }
            p = req.optString(name, "");
        }
        return p;
    }
}