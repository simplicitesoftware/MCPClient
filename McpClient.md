<!--
 ___ _            _ _    _ _    __
/ __(_)_ __  _ __| (_)__(_) |_ /_/
\__ \ | '  \| '_ \ | / _| |  _/ -_)
|___/_|_|_|_| .__/_|_\__|_|\__\___|
            |_| 
-->
![Logo](https://platform.simplicite.io/logos/standard/logo250.png)
* * *

`McpClient` module definition
=============================

Client module for internal use of Simplicité MCP server

Set AI_API_PARAM mistral exemple:
```json
{
 "completion_url": "https://api.mistral.ai/v1/chat/completions",
 "bot_name": "George",
 "code_max_token": "2000",
 "provider": "Mistral AI",
 "api_key": "[ENV:api_key]",
 "default_max_token": "5000",
 "showDataDisclaimer": true,
 "data_number": "3",
 "hist_depth": "3",
 "model": "mistral-medium-3-5",
 "stt_url": "",
 "ping_url": "https://api.mistral.ai/v1/models"
}
```

Claude:
```json
{
  "completion_url": "https://api.anthropic.com/v1/messages",
  "bot_name": "George",
  "code_max_token": "2000",
  "provider": "anthropic",
  "api_key": "[ENV:api_key]",
  "anthropic-workspace-id":"[ENV:workspace_id]",
  "default_max_token": "5000",
  "showDataDisclaimer": true,
  "data_number": "3",
  "hist_depth": "3",
  "model": "claude-sonnet-4-5-20250929",
  "stt_url": "",
  "ping_url": "https://api.anthropic.com/v1/models",
  "ClaudeAPI":true
}
```

Domains
-------

_Domains organize the application's main navigation menu. Each domain groups the objects, processes, and external pages accessible to users from it. Domains can be nested: a domain may have a parent domain._

* `McpcltDomaine` _(sub-domain of `McpDomain`)_

System parameters
-----------------

| Code | Value | Type | Description |
|---|---|---|---|
| `MCP_MUTE_TOOLS` | `false` |  |  |

`LlmProvider` (Provider) business object definition
---------------------------------------------------

_No description._

**Physical table**: `llm_provider`

### Fields

| Name                                                         | Type                                     | Column                         | Required | Updatable | Personal | Description                                                                      |
|--------------------------------------------------------------|------------------------------------------|--------------------------------|----------|-----------|----------|----------------------------------------------------------------------------------|
| `llmPrvProvider`                                             | enum(100) using LLM_PROVIDER list        | llm_prv_provider               | yes*     | yes       |          | -                                                                                |
| `llmPrvDefaultUrl`                                           | url(100)                                 | llm_prv_default_url            | yes      | yes       |          | -                                                                                |
| `llmPrvDataModel`                                            | text(4000)                               | llm_prv_data_model             | yes      | yes       |          | -                                                                                |
| `llmPrvHelp`                                                 | html(50000)                              | llm_prv_help                   |          | yes       |          | -                                                                                |
| `llmPrvUserParameters`                                       | text(5000)                               | llm_prv_user_parameters        |          | yes       |          | -                                                                                |
| `llmPrvModelsUrl`                                            | url(100)                                 | llm_prv_models_url             |          | yes       |          | -                                                                                |
| `llmPrvCompletionUrl`                                        | url(100)                                 | llm_prv_completion_url         |          | yes       |          | -                                                                                |
| `llmPrvPingUrl`                                              | url(100)                                 | llm_prv_ping_url               |          | yes       |          | -                                                                                |
| `llmPrvSttUrl`                                               | url(100)                                 | llm_prv_stt_url                |          | yes       |          | -                                                                                |
| `llmModel`                                                   | enum(100) using LLM_MODEL_SERVICE list   |                                |          | yes       |          | -                                                                                |
| `llmHistoryDepth`                                            | int(100)                                 |                                |          | yes       |          | -                                                                                |
| `llmMaxToken`                                                | int(100)                                 |                                |          | yes       |          | -                                                                                |
| `llmDataDisclamer`                                           | boolean                                  |                                |          | yes       |          | -                                                                                |
| `llmApiKey`                                                  | password(500)                            |                                |          | yes       |          | -                                                                                |
| `llmAnthropicWorkspaceId`                                    | password(100)                            |                                |          | yes       |          | -                                                                                |
| `llmGptProjectId`                                            | password(100)                            |                                |          | yes       |          | -                                                                                |
| `llmGptOrganizationId`                                       | password(100)                            |                                |          | yes       |          | -                                                                                |

### Enumerations

* `LLM_PROVIDER`
    - `mistral` Mistral AI
    - `claude` Anthropic Claude
    - `openai` Open Ai
    - `mammouth` Mammouth AI
    - `Albert` Albert API

### Description (from code)

> Business object LlmProvider

### Implemented hooks

* `postLoad`
* `preCreate`

External objects
----------------

* `AIChatBot`: Chat interface
* `AiMcpClientApi`: _No description._
* `AiMonitoring`: _No description._
* `LlmSetup`: _No description._

Shared code
-----------

* `LlmTools` _(Server script)_
* `McpClientManager` _(Server script)_

