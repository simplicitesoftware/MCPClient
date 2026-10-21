package com.simplicite.extobjects.McpClient; // NOSONAR - name of module

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.List;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import com.simplicite.util.Grant;
import com.simplicite.util.AppLog;
import com.simplicite.util.Tool;
import com.simplicite.util.Message;
import com.simplicite.util.tools.HTMLTool;
import com.simplicite.util.tools.AbstractMarkdownTool;
import com.simplicite.util.exceptions.HTTPException;
import com.simplicite.util.tools.Parameters;
import com.simplicite.commons.McpClient.LlmTools;
import com.simplicite.commons.McpClient.McpClientManager;

/** REST service external object AiMcpClientApi */
public class AiMcpClientApi extends com.simplicite.webapp.services.RESTServiceExternalObject {
    private static final long serialVersionUID = 1L;
    private McpClientManager manager; 
    private transient JSONArray tools;
    private String serverInstructions;
    private static final String PARAMS_PROMPT_KEY = "prompt";
    private static final String JSON_REQ_TYPE = "reqType";
    private boolean mcpMuteTools;
    private static final String RESPONSE_KEY = "response";

    private static final String FINISH_REASON_KEY = "finish_reason";
    private static final String DESC_KEY = "description";
    private static final String INDEX_KEY = "index";
    private static final String HIST_PARAM = "AI_CHAT_HIST";
    private static final String NOT_FOUND = "not found";
    @Override
    public void init(Parameters params) {
        Grant g = getGrant();
        AppLog.log(LlmTools.DEBUG, getClass(), "init", "init API with GRANT " + g.getLogin());
        manager = new McpClientManager(g);
        tools = manager.listToolsAsLlmFormat();
        serverInstructions = manager.getServerInstructions();
        String param = getGrant().getUserSystemParam("MCP_MUTE_TOOLS");
        if (Tool.isEmpty(param))
            param = Grant.getSystemAdmin().getParameter("MCP_MUTE_TOOLS");
        mcpMuteTools = "true".equals(param);
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
     * POST : envoie un prompt, retourne réponse ou demande d'action Body attendu :
     * { "prompt":
     * "...", "tool": "..." (optionnel) }
     */
    @Override
    public Object post(Parameters params) throws HTTPException {
        try {
            List<String> parts = params.getLocationParts();
            JSONObject req = params.getJSONObject();
            String prompt = getParamOrreqParam(PARAMS_PROMPT_KEY, params, req);
            String type = parts.size() > 3 ? parts.get(3) : getParamOrreqParam(JSON_REQ_TYPE, params, req);
            if (Tool.isEmpty(type))// NOSONAR - Tool.isEmpty check null
                type = "default";
            switch (type) {// NOSONAR - Tool.isEmpty check null
                case "provider":
                    return new JSONObject().put("provider", LlmTools.getProvider());
                case "chatBot":
                    return chatbotCaller(prompt, params, req);
                case "simpleChatBot":
                    String id = req.has("chat_id") ? req.getString("chat_id") : getGrant().getSessionId();
                    String msg = req.optString(LlmTools.MESSAGE_KEY);
                    return new JSONObject().put(LlmTools.MESSAGE_KEY, msg).put("reply", simpleChatbotCaller(msg, id));
                case "BOT_NAME":
                    return new JSONObject().put("botName", LlmTools.getBotName());
                case "CHECK_SPEECH_RECOGNITION":
                    return new JSONObject()
                            .put("isSpeechRecognitionSupported", LlmTools.checkSpeechRecognition());
                case "ping":
                    return ping();
                case "recallWithTools":
                    return recallWithTools(prompt, params, req);
                case "getModels":
                    List<String> models = LlmTools.getModels(req.optString("url"),req.optString("apiKey"),getGrant());
                    if(models.isEmpty() || "error".equals(models.get(0)))
                        return error(Integer.parseInt(models.get(1)),models.get(2));
                    return success(LlmTools.getModels(req.optString("url"),req.optString("apiKey"),getGrant()));
                case "setParams":
                    AppLog.info("setParams");
                    LlmTools.setParameters(req.optJSONObject("llmSettings"));
                    return ping();

                default:
                    AppLog.warning("mcp api type not found: " + type +" "+ params.toJSON());
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
        if (Tool.isEmpty(prompt))
            return "You said nothing :-(";
        JSONObject result = simpleChatbotCaller(prompt, id, null, null);
        JSONObject choice = result.optJSONObject(RESPONSE_KEY, new JSONObject()).optJSONArray(LlmTools.CHOICES_KEY, new JSONArray())
                .optJSONObject(0, new JSONObject());
        while (LlmTools.TOOL_CALLS_KEY.equals(choice.optString(FINISH_REASON_KEY))) {
            JSONArray toolCalls = choice.optJSONObject(LlmTools.MESSAGE_KEY, new JSONObject()).optJSONArray(LlmTools.TOOL_CALLS_KEY,
                    new JSONArray());
            result = simpleChatbotCaller(prompt, id, toolCalls, new JSONArray());
            choice = result.optJSONObject(RESPONSE_KEY, new JSONObject()).optJSONArray(LlmTools.CHOICES_KEY, new JSONArray())
                    .optJSONObject(0, new JSONObject());
        }
        if (result.optJSONObject(RESPONSE_KEY, new JSONObject()).has("error"))
            return getGrant().T("AI_ERROR");
        String res = choice.optJSONObject(LlmTools.MESSAGE_KEY, new JSONObject()).optString(LlmTools.CONTENT_KEY, getGrant().T("AI_ERROR"));
        return HTMLTool.toSafeHTML(AbstractMarkdownTool.toHTML(res));
    }

    private JSONObject simpleChatbotCaller(
            String prompt,
            String id,
            JSONArray acceptedTools,
            JSONArray refusedTools) {

        try {
            if (Tool.isEmpty(prompt)) {
                return badRequest("need a prompt");
            }
            JSONArray promptJson = new JSONArray().put(new JSONObject().put("type", "text").put("text", prompt.trim()));
            prompt = promptJson.toString();
            JSONArray historic = getHistoric(id);
            if (tools.length() == 0) {
                AppLog.warning("NO MCP TOOLS FOUND, Call api without tools");
                JSONObject result = LlmTools.aiCaller(getGrant(), serverInstructions, historic, prompt);
                addHist(id, result, prompt, null, null);
                return result;
            } else {
                JSONArray assistantToolsCalls = new JSONArray();
                JSONArray userToolsResponse = new JSONArray();
                JSONArray p = LlmTools.optJSONArray(prompt);
                if (acceptedTools != null && acceptedTools.length() > 0
                        || refusedTools != null && refusedTools.length() > 0) {
                        addAcceptedTools(acceptedTools, assistantToolsCalls, userToolsResponse);
                        addRefusedTools(refusedTools, assistantToolsCalls, userToolsResponse);
                }
                JSONObject response = LlmTools.aiCallerWithMCP(
                        getGrant(),
                        serverInstructions,
                        historic,
                        Tool.isEmpty(p) ? prompt : p,
                        tools,
                        assistantToolsCalls,
                        userToolsResponse);
                AppLog.log(LlmTools.DEBUG, getClass(), "simpleChatbotCaller", "LLM response: " + response.toString(1));
                JSONObject result = parseResponseTools(response,prompt);
                addHist(id, result, Tool.isEmpty(p) ? prompt : p, userToolsResponse, assistantToolsCalls);
                return result;
            }
        } catch (Exception e) {
            AppLog.error("McpClientApi POST error: " + e.getMessage(), e, getGrant());
            return error(e);
        }
    }
    private void addDescTools(JSONArray toolCall) {
        for (int i = 0; i < toolCall.length(); i++) {
            JSONObject tool = toolCall.getJSONObject(i);
            String toolName = tool.optJSONObject(LlmTools.TOOL_FUNCTION_KEY, new JSONObject())
                    .optString("name", "");
            String toolDescription = manager.getToolDescription(toolName);
            tool.put(DESC_KEY, toolDescription);
        }
    }
    private String getFinishReason(JSONObject response) {
        return response.optJSONArray(LlmTools.CHOICES_KEY, new JSONArray())
        .optJSONObject(0, new JSONObject())
        .optString(FINISH_REASON_KEY, "");
    }
    private JSONArray getTools(JSONObject response) {
        return response.getJSONArray(LlmTools.CHOICES_KEY)
        .getJSONObject(0)
        .optJSONObject(LlmTools.MESSAGE_KEY, new JSONObject())
        .optJSONArray(LlmTools.TOOL_CALLS_KEY, new JSONArray());
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
            return success(Message.formatInfo("AI_SUCCESS_PING", null, null));
        }
        return error(400,ping);
    }

    private JSONObject chatbotCaller(String prompt, Parameters params, JSONObject req) {
        JSONObject result = chatbotCaller(prompt, params, req, null, null);
        if (!mcpMuteTools) {
            return result;
        }
        JSONObject choice = result.optJSONObject(RESPONSE_KEY, new JSONObject()).optJSONArray(LlmTools.CHOICES_KEY, new JSONArray())
                .optJSONObject(0, new JSONObject());
        while (LlmTools.TOOL_CALLS_KEY.equals(choice.optString(FINISH_REASON_KEY))) {
            JSONArray toolCalls = choice.optJSONObject(LlmTools.MESSAGE_KEY, new JSONObject()).optJSONArray(LlmTools.TOOL_CALLS_KEY,
                    new JSONArray());
            result = chatbotCaller(prompt, params, req, toolCalls, new JSONArray());
            choice = result.optJSONObject(RESPONSE_KEY, new JSONObject()).optJSONArray(LlmTools.CHOICES_KEY, new JSONArray())
                    .optJSONObject(0, new JSONObject());
        }
        return result;
    }

    private JSONObject chatbotCaller(
            String prompt,
            Parameters params,
            JSONObject req,
            JSONArray acceptedTools,
            JSONArray refusedTools) {
        try {
            prompt = prompt.trim();
            if (Tool.isEmpty(prompt)) {
                return badRequest("need a prompt");
            }
            AppLog.log(LlmTools.DEBUG, getClass(), "chatbotCaller", "request: " + req.toString(1));
            String id = getParamOrreqParam("id", params, req);
            JSONArray historic = Tool.isEmpty(id) ? LlmTools.optJSONArray(getParamOrreqParam("historic", params, req))
                    : getHistoric(id);
            if (tools.length() == 0) {
                AppLog.warning("NO MCP TOOLS FOUND, Call api without tools");
                JSONObject result = LlmTools.aiCaller(getGrant(), serverInstructions, historic, prompt);
                addHist(id, result, prompt, null, null);
                return result;
            } else {
                JSONArray assistantToolsCalls = new JSONArray();
                JSONArray userToolsResponse = new JSONArray();
                JSONArray p = LlmTools.optJSONArray(prompt);
                if (acceptedTools != null && acceptedTools.length() > 0
                        || refusedTools != null && refusedTools.length() > 0) {
                    addAcceptedTools(acceptedTools, assistantToolsCalls, userToolsResponse);
                    addRefusedTools(refusedTools, assistantToolsCalls, userToolsResponse);
                }

                JSONObject response = LlmTools.aiCallerWithMCP(
                        getGrant(),
                        serverInstructions,
                        historic,
                        Tool.isEmpty(p) ? prompt : p,
                        tools,
                        assistantToolsCalls,
                        userToolsResponse);
                AppLog.log(LlmTools.DEBUG, getClass(), "chatbotCaller", "LLM response: " + response.toString(1));
                JSONObject result = parseResponseTools(response,prompt);
                addHist(id, result, Tool.isEmpty(p) ? prompt : p, userToolsResponse, assistantToolsCalls);
                return result;
            }
        } catch (Exception e) {
            AppLog.error("McpClientApi POST error: " + e.getMessage(), e, getGrant());
            return error(e);
        }
    }
    private JSONObject parseResponseTools(JSONObject response, String prompt) {
        JSONArray toolCall = new JSONArray();
        if (LlmTools.TOOL_CALLS_KEY
                    .equals(
                            getFinishReason(response))) {
            toolCall =getTools(response);
            addDescTools(toolCall);
        }
        return new JSONObject()
                    .put("tools", toolCall)
                    .put("request", prompt)
                    .put(RESPONSE_KEY, response);
    }
    private void addAcceptedTools(JSONArray acceptedTools, JSONArray assistantToolsCalls, JSONArray userToolsResponse) {
        for (int i = 0; i < acceptedTools.length(); i++) {

            JSONObject tool = acceptedTools.getJSONObject(i);
            if (tool.has(DESC_KEY)) {
                tool.remove(DESC_KEY);
            }
            if (tool.has(INDEX_KEY)) {
                tool.remove(INDEX_KEY);
            }
            tool.remove(INDEX_KEY);
            assistantToolsCalls.put(tool);
            JSONObject userToolResponse = new JSONObject();
            userToolResponse.put(LlmTools.CALL_ID_KEY, tool.optString("id"));
            userToolResponse.put("role", "tool");
            userToolResponse.put(LlmTools.CONTENT_KEY, getToolResponse(tool));
            userToolsResponse.put(userToolResponse);
            AppLog.log(LlmTools.DEBUG, LlmTools.class, "addAcceptedTools", "Accepted tool:" + tool.toString(1)+ "\nresult: "+userToolResponse.toString(1));
        }
    }
    private void addRefusedTools(JSONArray refusedTools, JSONArray assistantToolsCalls, JSONArray userToolsResponse) {
        for (int i = 0; i < refusedTools.length(); i++) {
            JSONObject tool = refusedTools.getJSONObject(i);
            if (tool.has(DESC_KEY)) {
                tool.remove(DESC_KEY);
            }
            if (tool.has(INDEX_KEY)) {
                tool.remove(INDEX_KEY);
            }
            tool.remove(INDEX_KEY);
            AppLog.log(LlmTools.DEBUG, LlmTools.class, "addRefusedTools", "Refused Tool" + tool.toString(1));
            assistantToolsCalls.put(tool);
            JSONObject userToolResponse = new JSONObject();
            userToolResponse.put(LlmTools.CALL_ID_KEY, tool.optString("id"));
            userToolResponse.put("role", "tool");
            userToolResponse.put(LlmTools.CONTENT_KEY,
                    "Tool execution denied by user. Ask the user what they would like to do now");
            userToolsResponse.put(userToolResponse);
        }
    }
    private void addHist(String id, JSONObject response, Object prompt, JSONArray toolsRep, JSONArray toolsCall) {
        boolean isToolCall = LlmTools.TOOL_CALLS_KEY
                .equals(response.optJSONObject(RESPONSE_KEY, new JSONObject()).optJSONArray(LlmTools.CHOICES_KEY, new JSONArray())
                        .optJSONObject(0, new JSONObject()).optString(FINISH_REASON_KEY, ""));
        Grant g = getGrant();
        JSONObject json = g.getJSONObjectParameter(HIST_PARAM, "{}");
        if (!json.has(id))
            json.put(id, new JSONArray());
        JSONArray hist = json.getJSONArray(id);
        String usermsg = getsStrPrompt(prompt); 
        JSONObject lastHist = hist.optJSONObject(hist.length() - 1);
        boolean hasToolContext  = isToolCall || !Tool.isEmpty(toolsCall); 
        boolean alreadyRecorded  = hasToolContext && !Tool.isEmpty(lastHist)  &&
        (
                "tool".equals(lastHist.optString("role", "")) || 
                (
                        "user".equals(lastHist.optString("role", "")) &&
                        usermsg.equals(lastHist.optString(LlmTools.CONTENT_KEY, ""))
                )
        );     
        if (!alreadyRecorded) {
            hist.put(new JSONObject().put("role", "user").put(LlmTools.CONTENT_KEY, usermsg));
        }
        if (!Tool.isEmpty(toolsCall) && !Tool.isEmpty(toolsRep)) {
            JSONObject calls = new JSONObject().put("role", LlmTools.ASSISTANT_ROLE).put(LlmTools.TOOL_CALLS_KEY, toolsCall)
                    .put(LlmTools.CONTENT_KEY, JSONObject.NULL);
            hist.put(calls);
            for (Object o : toolsRep) {
                hist.put(o);
            }
        }
        if (!isToolCall) {
            String botResponse = response.optJSONObject(RESPONSE_KEY, new JSONObject())
                    .optJSONArray(LlmTools.CHOICES_KEY, new JSONArray()).optJSONObject(0, new JSONObject())
                    .optJSONObject(LlmTools.MESSAGE_KEY, new JSONObject()).optString(LlmTools.CONTENT_KEY, NOT_FOUND);
            hist.put(new JSONObject().put("role", LlmTools.ASSISTANT_ROLE).put(LlmTools.CONTENT_KEY,
                    Tool.isEmpty(botResponse) ? NOT_FOUND : botResponse));
        }
        g.setParameter(HIST_PARAM, json.toString(1));
        g.setUserSystemParam(HIST_PARAM, json.toString(1), false);
    }
    private String getsStrPrompt(Object prompt) {

        if (prompt instanceof String strPrompt) {
            return strPrompt;
        } else if (prompt instanceof JSONArray arr) {
            for (Object o : arr) {
                JSONObject j = (JSONObject) o;
                if ("text".equals(j.optString("type", ""))) {
                    return cleanUserMsg(j.optString("text", NOT_FOUND));
                }
            }

        }
        return NOT_FOUND;
    }
    private String cleanUserMsg(String usermsg) {
        if (Tool.isEmpty(usermsg)) {
            return "";
        }
        if(usermsg.startsWith("\"")) {
            usermsg = usermsg.substring(1, usermsg.length());
        }
        if(usermsg.endsWith("\"")) {
            usermsg = usermsg.substring(0, usermsg.length() - 1);
        }
        return usermsg;
    }
    private String getToolResponse(JSONObject tool) {
        JSONObject toolfunc = tool.optJSONObject(LlmTools.TOOL_FUNCTION_KEY, new JSONObject());
        String toolName = toolfunc.optString("name", "");
        JSONObject parametersJson;
        Object parameters = toolfunc.opt("arguments");
        if (parameters instanceof JSONObject json) {
            parametersJson = json;
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

    private JSONArray getHistoric(String id) {
        JSONObject json = getGrant().getJSONObjectParameter(HIST_PARAM, "{}");
        if (json.has(id))
            return json.optJSONArray(id);
        return new JSONArray();
    }

    private String getParamOrreqParam(String name, Parameters params, JSONObject req) {
        if (Tool.isEmpty(name))
            return null;
        String p = params.getParameter(name);
        if (Tool.isEmpty(p) && req.has(name)) {
            Object op = req.get(name);
            switch (op) {
                case JSONArray arr:
                    p = arr.toString();
                    break;
                case JSONObject obj:
                    p = obj.toString();
                    break;
                default:
                    p = req.optString(name, "");
                    break;
            }   
        }
        return p;
    }

    @Override
    public void destroy() {
        manager.close();
    }
}