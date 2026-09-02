package com.simplicite.commons.McpClient;

import java.util.List;
import com.simplicite.util.AppLog;
import com.simplicite.util.Grant;
import com.simplicite.util.Globals;
import com.simplicite.util.Tool;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import java.net.http.HttpRequest;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Shared code McpClientManager
 * This class is used to manage the McpClient instance for the Simplicite MCP
 * serveur.
 * It provides methods to list tools, get tool description, call tools with
 * prompt, and close the client.
 * Used by the internal chatbot to interact with the MCP server.
 * through {@link com.simplicite.webapp.mcp.McpUiServlet} servlet.
 */
public class McpClientManager implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Singleton instance of the McpClient.
     */
    private transient McpSyncClient client;

    /**
     * Constructor of the McpClientManager.
     * 
     * @param g The grant object.
     */
    public McpClientManager(Grant g) {
        initHttpClientUi(g);
    }

    

    /**
     * Get the singleton instance of the McpClient.
     * 
     * @return The singleton instance of the McpClient.
     */
    public McpSyncClient getClient() {
        return client;
    }

    /**
     * List the tools available on the MCP server.
     * 
     * @return The list of tools.
     */
    public ListToolsResult listTools() {
        return client.listTools();
    }

    /**
     * Get the server instructions.
     * 
     * @return The server instructions.
     */
    public String getServerInstructions() {
        return client.getServerInstructions();
    }

    /**
     * List the tools available on the MCP server in llm format.
     * 
     * @return The list of tools in OpenAI format.
     */
    public JSONArray listToolsAsLlmFormat() {
        if(LlmTools.CLAUDE_LLM.equals(LlmTools.llm)) {
            return mcpToolsToClaudeFormat(listTools().tools());
        }else{
            return mcpToolsToOpenAIFormat(listTools().tools());
        }

    }
    /**
     * List the tools available on the MCP server in OpenAI format.
     * 
     * @return The list of tools in OpenAI format.
     */
    public JSONArray listToolsAsOpenAIFormat() {
        return mcpToolsToOpenAIFormat(listTools().tools());
    }


    /**
     * Convert the list of tools to OpenAI format.
     * 
     * @param mcpTools The list of tools.
     * @return The list of tools in OpenAI format.
     */
    private JSONArray mcpToolsToOpenAIFormat(List<io.modelcontextprotocol.spec.McpSchema.Tool> mcpTools) {
        JSONArray tools = new JSONArray();
        for (io.modelcontextprotocol.spec.McpSchema.Tool mcpTool : mcpTools) {
            JSONObject tool = new JSONObject();
            tool.put("type", "function");

            JSONObject function = new JSONObject();
            function.put("name", mcpTool.name());
            function.put("description", mcpTool.description() != null ? mcpTool.description() : "");
            JSONObject parameters;
            if (mcpTool.inputSchema() != null) {
                // inputSchema() returns an McpSchema.JsonSchema
                // convert to a JSONObject
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String schemaJson = mapper.writeValueAsString(mcpTool.inputSchema());
                    parameters = new JSONObject(schemaJson);
                } catch (Exception e) {
                    // fallback : empty schema
                    AppLog.warning("fallback : empty schema for tool " + mcpTool.name(), e);
                    parameters = new JSONObject()
                            .put("type", "object")
                            .put("properties", new JSONObject());
                }
            } else {
                parameters = new JSONObject()
                        .put("type", "object")
                        .put("properties", new JSONObject());
            }
            function.put("parameters", parameters);

            tool.put("function", function);
            tools.put(tool);
        }
        return tools;
    }

    /**
     * Convert the list of tools to Claude (Anthropic) format.
     * 
     * @param mcpTools The list of tools.
     * @return The list of tools in Claude format.
     */
    private static JSONArray mcpToolsToClaudeFormat(List<io.modelcontextprotocol.spec.McpSchema.Tool> mcpTools) {
        JSONArray tools = new JSONArray();
        for (io.modelcontextprotocol.spec.McpSchema.Tool mcpTool : mcpTools) {
            JSONObject tool = new JSONObject();
            tool.put("name", mcpTool.name());
            tool.put("description", mcpTool.description() != null ? mcpTool.description() : "");

            JSONObject inputSchema;
            if (mcpTool.inputSchema() != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String schemaJson = mapper.writeValueAsString(mcpTool.inputSchema());
                    inputSchema = new JSONObject(schemaJson);
                } catch (Exception e) {
                    AppLog.warning("fallback : empty schema for tool " + mcpTool.name(), e);
                    inputSchema = new JSONObject()
                            .put("type", "object")
                            .put("properties", new JSONObject());
                }
            } else {
                inputSchema = new JSONObject()
                        .put("type", "object")
                        .put("properties", new JSONObject());
            }
            tool.put("input_schema", inputSchema);

            tools.put(tool);
        }
        return tools;
    }

    /**
     * Call a tool with a prompt.
     * 
     * @param toolName  The name of the tool.
     * @param arguments The arguments of the tool.
     * @return The result of the tool call.
     */
    public CallToolResult callWithPrompt(String toolName, JSONObject arguments) {
        return client.callTool(CallToolRequest.builder(toolName)
                .arguments(arguments.toMap())
                .build());
    }

    /**
     * Close the McpClient.
     */
    public void close() {
        if (client != null) {
            try {
                client.closeGracefully();
            } catch (Exception e) {
                AppLog.warning("Close error", e);
            }
        }
    }

    /**
     * Get the description of a tool.
     * 
     * @param toolName The name of the tool.
     * @return The description of the tool.
     */
    public String getToolDescription(String toolName) {
        if (Tool.isEmpty(toolName))
            return null;
        if (client == null)
            return null;
        try {
            return client.listTools().tools().stream()
                    .filter(tool -> tool.name().equals(toolName))
                    .findFirst()
                    .map(io.modelcontextprotocol.spec.McpSchema.Tool::description)
                    .orElse("");
        } catch (Exception e) {
            AppLog.warning("Get tool description error", e);
            return null;
        }

    }

    /**
     * Initialize the McpClient.
     * 
     * @param g The grant object.
     */
    public void initHttpClientUi(Grant g) {
        // connect to the MCP server using the session ID
        String sessionId = g.getSession().getId();
        // create the transport
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder(g.getContextURL())
                .endpoint(Globals.WEB_UI_PATH + Globals.WEB_MCP_PATH)
                .requestBuilder(HttpRequest.newBuilder()
                        .header("Cookie", "JSESSIONID=" + sessionId).header("Cookie", "JSESSIONID=" + sessionId))
                .build();
        // create the client
        client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(30)).build();
        // initialize the client
        client.initialize();
    }
}