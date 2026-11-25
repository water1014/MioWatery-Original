# MioWatery 原生 AI 引擎迁移规格（源码核验版）

> 本文根据 `index.html` 中 `apiSessionId`、`apiRequestHeaders`、`modelListEndpoint`、`ask`、`build` 等函数整理。它记录旧版行为，原生实现必须在测试中明确兼容或说明差异。

## 1. 聊天请求协议

- 默认请求地址：去除 API Base 末尾斜杠后拼接 `/chat/completions`。
- 请求方法：POST。
- 默认请求头：
  - `Content-Type: application/json`
  - `Authorization: Bearer <API Key>`
- 请求体默认字段：
  - `model`
  - `temperature`
  - `stream`
  - `messages`
- 可选请求参数由调用方传入并合并到默认请求体。
- 非流式响应读取 `choices[0].message`。
- 流式响应使用 SSE 行读取，处理 `data:` 前缀，忽略 `[DONE]`，从 `choices[0].delta` 读取内容。

## 2. 自定义请求头与 OpenCode Go

- 自定义请求头存储为 JSON 字符串。
- 必须解析为非数组 JSON 对象；格式错误时应阻止请求并提示用户。
- 非空自定义 header 值会覆盖默认 header 中同名字段。
- 如果 API Base 包含 `opencode.ai`，且没有提供 `x-opencode-session`，旧版会自动附加该 header。
- 用户自定义的 `x-opencode-session` 值支持替换 `$SESSION_ID` 或 `{{SESSION_ID}}`。
- 会话 ID 按 `activeUserProfile:当前聊天角色ID` 分区保存，生成后复用。
- OpenCode Go 原生迁移必须保留按用户与聊天隔离的稳定 session ID；不得每次请求随机生成新值。

## 3. 模型列表地址

旧版对特定 OpenCode 路径有特殊处理：

- Base 以 `/inference/go/openai/v1` 结尾：将 `/openai/v1` 替换为 `/v1` 后追加 `/models`。
- Base 以 `/inference/go` 结尾：追加 `/v1/models`。
- 其他 Base：追加 `/models`。

原生版应将“聊天请求地址”和“模型列表地址”分开计算，不可简单复用同一 URL。

## 4. 推理内容处理

非流式响应中，旧版优先读取 `reasoning_content`、`reasoning`、`thinking` 或 `reasoning_text`；若正文中含 `<think>...</think>` 或 `<thinking>...</thinking>`，也会拆出推理段。

流式响应会累计 delta 中的 `reasoning_content` / `reasoning` / `thinking`，与普通 `content` 分开；聊天记录可能保存 `reasoning` 字段。

原生版需支持：
- 正文流式更新。
- 推理内容独立累计和折叠展示。
- 取消请求（OkHttp Call cancel / coroutine cancellation）。
- HTTP 错误响应尽量读取服务端 `error.message`，并保留 HTTP 状态码。

## 5. 提示词构建职责

旧版 `build(persona, history, split, options)` 并非简单拼接角色 prompt，包含：

1. 角色人设 prompt。
2. 用户资料中的 bio。
3. 线上预设（按角色选择）。
4. 角色级系统提示词。
5. 传统记忆。
6. 输出语言设置。
7. 最近记忆条目、合并的早期记忆。
8. 线下上下文、情侣空间、NPC、共读等模块上下文。
9. 世界书触发条件、作用范围、插入位置。
10. 每日回忆与时间间隔提示。
11. 按上下文长度截取的聊天历史。
12. 多模态消息内容（图片 URL / 图片数据）。

原生版应把提示词组装拆成可测试的独立组件，例如：
- `PersonaPromptComposer`
- `MemoryContextProvider`
- `WorldbookMatcher`
- `FeatureContextProvider`
- `ConversationHistoryReducer`

各组件应返回结构化片段和来源标签，便于查看“本轮发送给模型的内容”，同时保留旧版提示词优先级和插入顺序。

## 6. 第一阶段原生网络层接口建议

- `ChatCompletionClient`：执行一次非流式/流式聊天请求。
- `ApiHeaderResolver`：默认 header、自定义 header 和 OpenCode session 处理。
- `ModelCatalogClient`：模型列表 URL 与解析。
- `ReasoningDeltaParser`：正文/推理分离。
- `PromptComposer`：组装 system prompt、世界书、记忆和历史。
- `ChatRepository`：负责会话持久化、消息排序和请求状态。

API Key 必须来自加密凭据仓库，不直接写入 Room 的普通配置表或日志。

## 7. 兼容性测试基线

- [ ] OpenAI 兼容非流式聊天。
- [ ] OpenAI 兼容 SSE 流式聊天。
- [ ] 自定义 header JSON 格式错误提示。
- [ ] 自定义 header 覆盖默认值。
- [ ] OpenCode Go 自动 session header。
- [ ] `$SESSION_ID` / `{{SESSION_ID}}` 替换。
- [ ] 同一用户同一聊天复用 session ID。
- [ ] 不同用户或不同聊天隔离 session ID。
- [ ] OpenCode 特殊模型列表 URL。
- [ ] reasoning 字段和 think 标签解析。
- [ ] 请求取消、超时和网络断开。
- [ ] 图片消息与未知扩展字段保留。
