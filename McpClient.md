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

External objects
----------------

* `AiMcpClientApi`: _No description._
* `AiMonitoring`: _No description._

Shared code
-----------

* `LlmTools` _(Server script)_
* `McpClientManager` _(Server script)_

