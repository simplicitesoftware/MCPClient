package com.simplicite.extobjects.McpClient; // NOSONAR - Package name imposed by module name.

import com.simplicite.util.tools.Parameters;
import org.json.JSONObject;
import com.simplicite.commons.McpClient.LlmTools;
/**
 * UI component external object LlmSetup
 */
public class LlmSetup extends com.simplicite.webapp.web.ResponsiveExternalObject {
    private static final long serialVersionUID = 1L;

    // Note: in most cases no server-side Java code is needed
    // Just implement the 3 client-side resources: HTML, SCRIPT (Javascript with the render function), STYLES

    @Override
    public void init(Parameters params) {

        addSimpliciteClient();
    }
    @Override
     public JSONObject data(Parameters params) {
         JSONObject data = super.data(params);
         if(!LlmTools.isConfigurable()) data.put("setupEnv",true);
         else if (LlmTools.isSetup()){
             data.put("update",LlmTools.getCurrentSetup());
         }
         return data;
     }

}
