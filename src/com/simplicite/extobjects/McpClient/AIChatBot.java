package com.simplicite.extobjects.McpClient; // NOSONAR - Package name imposed by module name.



import com.simplicite.commons.McpClient.LlmTools;
import com.simplicite.util.ModuleDB;
import com.simplicite.util.Tool;
import com.simplicite.util.AppLog;
import com.simplicite.util.Grant;
import com.simplicite.util.tools.Parameters;
import com.simplicite.util.tools.HTMLTool;
import java.util.UUID;


/** External object AIChatBot */
public class AIChatBot extends com.simplicite.util.ExternalObject {
    private static final long serialVersionUID = 1L;

    /**
     * Display method
     *
     * @param params Request parameters
     */
    @Override
    public Object display(Parameters params) {
        Grant g = getGrant();
        if (!LlmTools.isAIParam(true))
            return javascript(
                    "$ui.alert({ content: \""
                            + g.T("AI_SETTING_NEED")
                            + "\", type: \"warning\" });");
        try {
            addMustache();
            addMarkdown();
            addSimpliciteClient();
            appendCSSInclude(HTMLTool.getResourceCSSURL(g, "AI_STYLE"));
            appendJSInclude(HTMLTool.getResourceJSURL(g, "AiJsTools"));
            String id = initHist();
            setTitle(false);
            return javascript(
                    getName()
                            + ".render(ctn,"
                            + isAdaContext()
                            + ",\"\",\""
                            + LlmTools.getDataDisclaimer(g)
                            + "\",'"
                            + id
                            + "');");
        } catch (Exception e) {
            AppLog.error(null, e, g);
            return e.getMessage();
        }
    }

    private String isAdaContext() {
        return (Tool.isEmpty(ModuleDB.getModuleId("AiDemonstrationAddon", false)))
                ? "false"
                : "true";
    }

    private String initHist() {

        return UUID.randomUUID().toString();
    }
}
