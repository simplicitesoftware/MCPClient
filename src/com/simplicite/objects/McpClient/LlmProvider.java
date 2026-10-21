package com.simplicite.objects.McpClient; // NOSONAR - Package name imposed by module name.


import com.simplicite.util.ObjectDB;
import com.simplicite.util.ObjectField;


/** Business object LlmProvider */
public class LlmProvider extends ObjectDB {
    private static final long serialVersionUID = 1L;

    @Override
    public void postLoad() {
        if ("setup".equals(getInstanceName())) {
            for (ObjectField f : getFields()) {
                if (!"llmPrvProvider".equals(f.getName()) && !"llmApiKey".equals(f.getName())) {
                    f.setVisibility(ObjectField.VIS_HIDDEN);
                } else {
                    f.setVisibility(ObjectField.VIS_BOTH);
                }
                if ("llmApiKey".equals(f.getName())) f.setPlaceholder("xxx");
            }
        }
    }

    @Override
    public String preCreate() {

        super.preCreate();
        String baseUrl = getFieldValue("llmPrvDefaultUrl");
        ObjectField completion = getField("llmPrvCompletionUrl");
        ObjectField model = getField("llmPrvModelsUrl");
        ObjectField ping = getField("llmPrvPingUrl");
        if (completion.isEmpty()) completion.setValue(baseUrl + "chat/completions");
        if (model.isEmpty()) model.setValue(baseUrl + "models");
        if (ping.isEmpty()) ping.setValue(baseUrl + "models");
        
        return null;
    }
}
