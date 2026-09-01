package com.simplicite.extobjects.McpClient;

import org.json.JSONObject;

import com.simplicite.commons.McpClient.LlmTools;
import com.simplicite.util.*;
import com.simplicite.util.tools.*;


/**
 * Basic external object AiMonitoring
 */
public class AiMonitoring extends com.simplicite.util.ExternalObject {
    private static final long serialVersionUID = 1L;

    // Note: instead of this basic external object, a specialized subclass should be used

    /**
     * Display method
     * @param params Request parameters
     */
    @Override
    public Object display(Parameters params) {
        try {
            // Call the render Javascript method implemented in the SCRIPT resource
            // ctn is the "div.extern-content" to fill on UI
            addSimpliciteClient();
            appendJSInclude(HTMLTool.getResourceJSURL(getGrant(), "AiJsTools"));
            if(!LlmTools.isAIParam(false)){
                return javascript(getName() + ".renderAINotParam(ctn);");
            }
            JSONObject aiParams = LlmTools.getParameters(true,getGrant().getLang());
            addMustache();
            return javascript(getName() + ".render(ctn,"+aiParams.toString()+");");
        }
        catch (Exception e) {
            AppLog.error(null, e, getGrant());
            return e.getMessage();
        }
    }
}