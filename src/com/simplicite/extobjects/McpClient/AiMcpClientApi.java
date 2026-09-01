package com.simplicite.extobjects.McpClient;


import org.json.JSONObject;
import org.json.JSONArray;
import  java.util.List;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import com.simplicite.util.Grant;
import com.simplicite.util.AppLog;
import com.simplicite.util.Tool;
import com.simplicite.util.Message;
import com.simplicite.util.tools.HTMLTool;
import com.simplicite.util.tools.MarkdownTool;
import com.simplicite.util.exceptions.HTTPException;
import com.simplicite.util.tools.Parameters;
import com.simplicite.commons.McpClient.LlmTools;
import com.simplicite.commons.McpClient.McpClientManager;


/** REST service external object AiMcpClientApi */
public class AiMcpClientApi extends com.simplicite.webapp.services.RESTServiceExternalObject {
    private static final long serialVersionUID = 1L;
    private static McpClientManager
            manager; // = McpClientManager.getInstance(Grant.getSystemAdmin());
    private static JSONArray tools; // = manager.listToolsAsOpenAIFormat();
    private static String serverInstructions;

    private static final String PARAMS_PROMPT_KEY = "prompt";
    private static final String JSON_REQ_TYPE = "reqType";
    public static  boolean mcpMuteTools;


    @Override
    public void init(Parameters params) {
        Grant g = getGrant();

        if (LlmTools.AI_DEBUG_LOGS) AppLog.info("init API with GRANT " + g.getLogin());
        manager = McpClientManager.getInstance(g);
        tools = manager.listToolsAsOpenAIFormat();
        serverInstructions = manager.getServerInstructions();
        String param = getGrant().getUserSystemParam("MCP_MUTE_TOOLS");
        if(Tool.isEmpty(param)) param = Grant.getSystemAdmin().getParameter("MCP_MUTE_TOOLS");
        mcpMuteTools="true".equals(param);
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

        if (LlmTools.AI_DEBUG_LOGS) {
            AppLog.info("McpClientApi POST tools: " + tools.toString(1), getGrant());
        }
        try {
            List<String> parts = params.getLocationParts();
            JSONObject req = params.getJSONObject();
            String prompt = getParamOrreqParam(PARAMS_PROMPT_KEY, params, req);
            String type = parts.size() > 3? parts.get(3): getParamOrreqParam(JSON_REQ_TYPE, params, req);
            if (Tool.isEmpty(type))
                type = "default";

            switch (type) { // use switch for future extension
                case "provider":
                    return new JSONObject().put("provider", LlmTools.provider());
                case "chatBot":
                    return chatbotCaller(prompt, params, req);
                case "simpleChatBot":
                    String id = req.has("chat_id")?req.getString("chat_id"):getGrant().getSessionId();
                    String msg = req.optString("message");
                     return new JSONObject().put("message", msg).put("reply", simpleChatbotCaller(msg,id));
                    //return ;
                case "BOT_NAME":
                    return new JSONObject().put("botName", LlmTools.getBotName());
                case "CHECK_SPEECH_RECOGNITION":
                    return new JSONObject()
                            .put("isSpeechRecognitionSupported", LlmTools.checkSpeechRecognition());
                case "ping":
                    return ping();
                case "recallWithTools":
                    return recallWithTools(prompt, params, req);
                default:
                    AppLog.info("AI API ERROR: " + type + params.toJSON());
                    return error(
                            400,
                            "Call me with a predefined request type please!");
            }

        } catch (Exception e) {
            AppLog.error(null, e, getGrant());
            return error(e);
        }
    }
    private String simpleChatbotCaller(String prompt, String id) {
        if(Tool.isEmpty(prompt)) return "You said nothing :-(";
        JSONObject result = simpleChatbotCaller(prompt,id,null,null);
        JSONObject choice = result.optJSONObject("response", new JSONObject()).optJSONArray("choices", new JSONArray()).optJSONObject(0, new JSONObject());

        while("tool_calls".equals(choice.optString("finish_reason"))){

            JSONArray toolCalls = choice.optJSONObject("message",new JSONObject()).optJSONArray("tool_calls", new JSONArray());
            result = simpleChatbotCaller(prompt,id, toolCalls, new JSONArray());
            choice = result.optJSONObject("response", new JSONObject()).optJSONArray("choices", new JSONArray()).optJSONObject(0, new JSONObject());
        }
        if(result.optJSONObject("response", new JSONObject()).has("error"))return getGrant().T("AI_ERROR");
        String res= choice.optJSONObject("message", new JSONObject()).optString("content", getGrant().T("AI_ERROR"));
        return   HTMLTool.toSafeHTML(MarkdownTool.toHTML(res));


    }
    private JSONObject simpleChatbotCaller(
        String prompt,
        String id,
        JSONArray acceptedTools,
        JSONArray refusedTools) {
    boolean istool = false;
    try {
        if (Tool.isEmpty(prompt)) {
            return badRequest("need a prompt");
        }
        JSONArray promptJson = new JSONArray().put(new JSONObject().put("type", "text").put("text", prompt.trim()));
        prompt = promptJson.toString();
        if (LlmTools.AI_DEBUG_LOGS) {
            AppLog.info("McpClientApi POST request", getGrant());
        }

        JSONArray historic = getHistoric(id);
        if (tools.length() == 0) {
            AppLog.warning("NO MCP TOOLS FOUND, Call api without tools");
            JSONObject result = LlmTools.aiCaller(getGrant(), serverInstructions, historic, prompt);
            addHist(id,result,prompt,null,null);
            return result;
        } else {

            JSONArray assistantToolsCalls = new JSONArray();
            JSONArray userToolsResponse = new JSONArray();
            JSONArray p = LlmTools.optJSONArray(prompt);
            if (acceptedTools != null && acceptedTools.length() > 0
                    || refusedTools != null && refusedTools.length() > 0) {

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
                    userToolResponse.put(LlmTools.CONTENT_KEY, getToolResponse(tool));
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
                    userToolResponse.put(LlmTools.CONTENT_KEY, "Tool execution denied by user. Ask the user what they would like to do now");
                    userToolsResponse.put(userToolResponse);
                }
            }
            if(istool ){
                int index = historic.length() -1;
                if(index >= 0 && "assistant".equals(historic.getJSONObject(index).getString("role"))) historic.remove(index);

            }
            JSONObject response =
                    LlmTools.aiCallerWithMCP(
                            getGrant(),
                            serverInstructions,
                            historic,
                            Tool.isEmpty(p) ? prompt : p,
                            tools,
                            assistantToolsCalls,
                            userToolsResponse);
            if (LlmTools.AI_DEBUG_LOGS) {
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
        String ping = LlmTools.pingAI();
        boolean isSuccess = LlmTools.PING_SUCCESS.equals(ping);
        if (isSuccess) {
            ping = Message.formatInfo("AI_SUCCESS_PING", null, null);
        }
        return new JSONObject().put("msg", ping);
    }

    private JSONObject chatbotCaller(String prompt, Parameters params, JSONObject req) {
        JSONObject result = chatbotCaller(prompt, params, req, null, null);
        if(!mcpMuteTools){
            return result;
        }
        JSONObject choice = result.optJSONObject("response", new JSONObject()).optJSONArray("choices", new JSONArray()).optJSONObject(0, new JSONObject());

        while("tool_calls".equals(choice.optString("finish_reason"))){

            JSONArray toolCalls = choice.optJSONObject("message",new JSONObject()).optJSONArray("tool_calls", new JSONArray());
            result = chatbotCaller(prompt, params, req, toolCalls, new JSONArray());
            choice = result.optJSONObject("response", new JSONObject()).optJSONArray("choices", new JSONArray()).optJSONObject(0, new JSONObject());
        }
        return result;
    }

    private JSONObject chatbotCaller(
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
            if (LlmTools.AI_DEBUG_LOGS) {
                AppLog.info("McpClientApi POST request: " + req.toString(1), getGrant());
            }
            String id = getParamOrreqParam("id",params,req);
            JSONArray historic = Tool.isEmpty(id)?LlmTools.optJSONArray(getParamOrreqParam("historic", params, req)):getHistoric(id);
            if (tools.length() == 0) {
                AppLog.warning("NO MCP TOOLS FOUND, Call api without tools");
                JSONObject result = LlmTools.aiCaller(getGrant(), serverInstructions, historic, prompt);
                addHist(id,result,prompt,null,null);
                return result;
            } else {

                JSONArray assistantToolsCalls = new JSONArray();
                JSONArray userToolsResponse = new JSONArray();
                JSONArray p = LlmTools.optJSONArray(prompt);
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
                        userToolResponse.put(LlmTools.CONTENT_KEY, getToolResponse(tool));
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
                        userToolResponse.put(LlmTools.CONTENT_KEY, "Tool execution denied by user. Ask the user what they would like to do now");
                        userToolsResponse.put(userToolResponse);
                    }
                }
                if(istool ){
                    int index = historic.length() -1;
                    if("assistant".equals(historic.getJSONObject(index).getString("role"))) historic.remove(index);

                }
                JSONObject response =
                        LlmTools.aiCallerWithMCP(
                                getGrant(),
                                serverInstructions,
                                historic,
                                Tool.isEmpty(p) ? prompt : p,
                                tools,
                                assistantToolsCalls,
                                userToolsResponse);
                if (LlmTools.AI_DEBUG_LOGS) {
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
        boolean isToolCall ="tool_calls".equals(response.optJSONObject("response",new JSONObject()).optJSONArray("choices", new JSONArray()).optJSONObject(0, new JSONObject()).optString("finish_reason", ""));
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
        if(isToolCall || !Tool.isEmpty(toolsCall)){
            JSONObject lastHist = hist.optJSONObject(hist.length()-1);
            if(Tool.isEmpty(lastHist) ||(!"tool".equals(lastHist.optString("role","")) && !("user".equals(lastHist.optString("role","")) && usermsg.equals(lastHist.optString(LlmTools.CONTENT_KEY,"")))))hist.put(new JSONObject().put("role","user").put(LlmTools.CONTENT_KEY,usermsg));

        }else {
           hist.put(new JSONObject().put("role","user").put(LlmTools.CONTENT_KEY,usermsg));

        }
        if(!Tool.isEmpty(toolsCall) && !Tool.isEmpty(toolsRep)){
            JSONObject calls = new JSONObject().put("role","assistant").put("tool_calls",toolsCall).put(LlmTools.CONTENT_KEY,JSONObject.NULL);
            hist.put(calls);
            for (Object o:toolsRep) {
                hist.put(o);  
            }
        }
        if(!isToolCall){ 
            String botResponse = response.optJSONObject("response",new JSONObject()).optJSONArray("choices",new JSONArray()).optJSONObject(0,new JSONObject()).optJSONObject("message",new JSONObject()).optString(LlmTools.CONTENT_KEY,"not found");
            hist.put(new JSONObject().put("role","assistant").put(LlmTools.CONTENT_KEY,Tool.isEmpty(botResponse)?"not found":botResponse));
       }
        g.setParameter("AI_CHAT_HIST",json.toString(1));
        g.setUserSystemParam("AI_CHAT_HIST",json.toString(1),false);
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