# Native Android feature completion matrix

Checked: 2026-11-25
Target branch: `native-android`
Reference repositories:
- Upstream: https://github.com/water649/MioWatery
- User fork: https://github.com/water1014/MioWatery

This matrix distinguishes a working native feature from a navigation placeholder. A visible app icon or route is not counted as implemented. The target is a faithful native recreation of MioWatery's own Mio small-phone structure. Do not add unrelated apps or duplicate shortcuts. Upstream feature groups and names take precedence over this implementation's earlier guesses.

## Current implementation audit

| App / area | Current native state | Completion requirement |
|---|---|---|
| Home / desktop | Partial, expanded | Four swipe pages and fixed dock; page 2 recent-search and quote/photo widgets, page 3 editable iMessage preview and music entry card, page 4 editable lyric text card. These user-edited values and photo URIs persist locally. Weather, memo/photo card on page 1, exact native music playback binding and visual parity remain. |
| Chat / messages | Partial | Local text chat, OpenAI-compatible API transport, streaming parser and reasoning storage exist. Attachments, multimodal payloads, robust cancellation, conversation list/search and full chat controls remain. |
| Contacts / personas | Partial | Persona list, editor, avatar selection and persona-specific chat entry exist. Full profile fields, import/export, deletion/management and parity remain. |
| Worldbook | Partial | CRUD, global/persona scope, trigger keywords and prompt injection exist. Full legacy schema parity, book-level management/import/export and advanced activation rules remain. |
| Desktop customization | Partial | Safe theme-pack import/preview/apply/export exists. Exact placement and full built-in widget compatibility remain. |
| Backup / migration | Partial | JSON/ZIP preview and confirmed import for selected core records exist. Full data-type coverage, round-trip export and upgrade preservation verification remain. |
| Diary | Partial (first native implementation) | Local create/edit/delete, date sorting and Room persistence. Search, annotations, image attachments, export and calendar integration remain. Keep the diary scope aligned with upstream; do not invent mood/favorite/lock features. |
| Music | Placeholder, desktop hooks added | Desktop page 3 and 4 show an honest 未在播放 state and open the Music route; lyrics text can be edited and persisted. Actual library, local media import, background playback, queue, playback controls and notification controls remain. |
| Couple space | Placeholder | Relationship timeline, shared records and configurable relationship start date. |
| Co-reading | Placeholder | Books, reading progress, notes and shared reading sessions. |
| Reading | Placeholder | Book library, reader, bookmarks and progress. |
| Messages | Partial, functional core | Independent 传讯 / 匿名问答 tabs; persona selection; AI-generated SMS and replies; named/anonymous questions; AI-generated anonymous question batches; delete; Room persistence with v2→v3 non-destructive migration. Notification/deep-link parity, full source UI and cross-profile persona binding remain. |
| Wallet | Placeholder | Ledger accounts, categories, income/expense records, budgets and summaries. |
| MCP | Placeholder | Server configuration, connection state, tool discovery, consent and safe tool invocation. |
| Presets | Placeholder | Prompt/configuration preset CRUD, import/export and selection in chat. |
| Life | Placeholder | Daily-life dashboard and linked routines/records. |
| IF line | Placeholder | Branch/story creation, timeline and isolated branch chat state. |
| Xinji / mood | Placeholder | Mood records, calendar view, tags and summaries. |
| Repository | Placeholder | Browse/import/upload flows with permissions and failure handling. |
| YouTube | Placeholder | Open external links or supported embedded playback; no fake in-app playback. |
| Offline | Placeholder | Define actual upstream feature behavior, then port supported offline workflows. |
| Role phones | Placeholder | Per-persona virtual phone data and independent app surfaces. |
| CSS / beautify | Placeholder | Native theme/material controls; do not execute imported CSS or scripts. |
| Stickers | Placeholder | Sticker library, import, picker and chat message integration. |
| Settings | Partial | API config and headers exist in chat flow; consolidate into a real settings hub with model, appearance, permissions, data and service settings. |
| Data / backup | Partial | Core legacy backup preview/import exists; full export, restore, validation and recovery flow remain. |

## Delivery order

### Foundation
1. Keep all native work isolated on `native-android`; never modify upstream or web-fork main as part of native work.
2. Establish a shared app shell, route registry, design tokens, loading/empty/error states and consistent back behavior.
3. Establish a durable storage strategy and versioned migrations before adding many feature tables.
4. Define a feature-level test checklist and require a successful debug build for every coherent batch.

### Core daily-use features
1. Chat, conversation management, attachments, cancellation and API settings.
2. Personas and worldbook parity, including import/export and prompt injection tests.
3. Diary and calendar-linked records.
4. Music library and reliable Android background playback.
5. Wallet / ledger with calculations and data export.

### Remaining apps
Implement each remaining app against upstream behavior and data schema, preserving unknown legacy fields. Where external services or unsupported web-only behavior are involved, show an honest native-compatible alternative and its limits.

### Release and device verification
- Verify clean install and upgrade install separately.
- Verify local database and files survive app upgrades.
- Verify backup export -> clear/reinstall -> restore round-trip on test data.
- Verify Android 16 permissions, photo/file picker, notifications, background playback, keyboard, status/navigation bars and process recreation.
- Test on the target vivo S19 device before describing a feature as device-verified.

## Definition of done

A feature is complete only when its primary actions work, data persists after process restart, empty/loading/error states are handled, navigation is correct, accessibility labels exist for important controls, and automated tests/build pass. A placeholder route does not satisfy this definition.


## Source-fidelity audit and desktop correction (2026-11-25)

The upstream `index.html` desktop was rechecked directly. The earlier cleanup that removed Wallet, MCP, Presets, Life, IF line, Xinji, Repository, YouTube and Offline was incorrect: all of these are real original desktop shortcuts. They have been restored to the native route registry and grouped by their original desktop pages.

The upstream desktop contains four swipe pages plus a four-item fixed dock:

| Desktop position | Original shortcuts |
|---|---|
| Dock | 聊天 (`app`), 世界书 (`worlds`), 联系人设定 (`personas`), 设置 (`settings`) |
| Page 2 | 日记 (`diary`), iPhone (`rolephones`), 桌面美化 (`desktopcustom`), 音乐 (`music`) |
| Page 3 | 情侣空间 (`couple`), 共读 (`coread`), 线下 (`offline`), 讯息 (`msgs`) |
| Page 4 | 钱包 (`wallet`), MCP (`mcp`), 预设 (`preset`), 生活 (`life`), IF线 (`iff`), 心迹 (`xinji`), 仓库 (`repo`), YouTube (`youtube`) |

The first desktop page is the clock / weather / recent-message widget page. Native implementation currently retains a provisional version and still needs visual and interaction parity work.

Current page-by-page gaps are explicit: page 1 still has a non-persistent memo-image placeholder, a non-configurable weather placeholder and an empty recent-message state; page 2 does not yet port the original recent-search and quote/photo widgets; page 3 currently shows its four app shortcuts but not the music mini-player or editable iMessage preview; page 4 currently shows its eight app shortcuts but not the original music/lyrics widget. These are remaining parity tasks, not completed features.

- `app` and `msgs` are distinct upstream routes. Native `app` opens chat; `msgs` now has its own route and is explicitly not redirected to ChatScreen. The message inbox itself is still a placeholder.
- The native desktop app grids now use the upstream page grouping and no longer display the invented generic "应用" title, calendar page, or "常用应用" shortcut card.
- Theme-pack widgets are no longer rendered as extra cards on the first clock/weather page; they are placed below the original first app-grid page, pending exact positional mapping from the source desktop layout.
- The four fixed dock entries are excluded from the three app-grid pages to avoid duplicate icons.
- The original source also contains CSS beautification, stickers management and data backup as internal/settings/chat workflows. They should not be promoted to invented desktop shortcuts without evidence.
- Backup remains available during migration work and should ultimately be placed under the original Settings/Data flow.
- This is a navigation and grouping correction, not a claim that the listed app features are already implemented. Placeholder modules remain clearly marked as incomplete.


## Batch progress: desktop widgets and Messages (2026-11-25)

- Page 2: Recent Searches supports three editable entries; quote/photo card supports editable copy and three image slots. Text and URI values persist in app-private SharedPreferences; image URI grants are persisted where the document provider supports it.
- Page 3: restored the mini-player card location with a truthful 未在播放 state and an entry to Music. Added editable iMessage preview title/subtitle and four alternating bubbles, persisted locally.
- Page 4: added editable two-line music/lyrics card and Music entry. It is a presentation widget only; no fake playback progress is shown.
- Messages: independent Compose screen implements source tabs 传讯 and 匿名问答. It calls the existing OpenAI-compatible client using saved API config and custom headers, scopes records to selected persona, and persists SMS / QA / anonymous batch entries in Room. Database migration 2→3 creates `message_app_entries`; no destructive migration is used.
- Remaining fidelity: Messages source has historical SMS navigation/soft keyboard and detailed bubble styling; current native version has the functional flows but not pixel-level UI parity. User-profile isolation beyond the currently active default profile remains to be completed.
## Follow-up correction: music player and functional widgets (2026-11-25)

The previous batch-progress paragraph above is superseded where it described the mini-player and Messages as placeholders:
- Page 2 Recent Searches has three editable, locally persisted entries. The quote/photo card has editable copy and three image slots; image URI permissions are persisted when the document provider supports it.
- Page 3 mini-player now reads saved local track name, cover and playback state. The iMessage preview title/subtitle and four alternating bubbles are editable and persisted.
- Page 4 lyrics/music card reads the two lines saved in the Music screen and links into the player.
- Music now has a native Media3 screen supporting a local audio picker, play/pause, replay, seek, cover selection and editable two-line lyrics. Track URI, title, cover URI and lyrics persist locally. Playback stops when leaving the screen. Online QQ Music search/catalog, playlists, automatic lyric matching and background MediaSession/foreground service remain incomplete.
- Messages is no longer a placeholder: its independent Compose screen implements 传讯 and 匿名问答, uses saved API config/custom headers, includes up to 12 recent messages from the selected persona's latest chat as context, and persists SMS / QA / anonymous batch entries in Room. Migration 2→3 creates message_app_entries without clearing existing data.
- Remaining Messages gaps: the current native implementation uses the default profile rather than full upstream multi-profile isolation; anonymous questions are generated as a batch rather than using the original per-question receive/selection flow; exact SMS navigation and bubble styling still need parity work.

## 首页组件实际数据接入（2026-11-25）

- 首页备忘录图片现在使用 Android 图片选择器，图片 URI 和授权状态尽可能持久化；重启后会尝试继续显示已选图片。
- 首页最近消息卡已连接 Room 查询，展示数据库中最近三条真实聊天消息，不再固定显示空状态；点击可进入聊天。
- 天气卡已接入 Open-Meteo 地理编码和当前天气接口，城市名称可编辑并保存；请求失败时展示错误状态，不伪造温度。天气服务需要网络连接。
- 这些是原生首页功能接入，不代表桌面视觉已经达到上游像素级一致；天气图标、原版卡片比例与动效仍需对照原版继续校准。

## 记忆功能与原版对照（2026-11-25）

- 已按上游 MioWatery 的实际结构修正：记忆管理属于单个聊天的聊天设置，不新增桌面独立应用入口。
- 每个 conversation 独立保存记忆，Room v3→v4 非破坏性迁移；不同聊天之间不会共用记忆记录。
- 当前原生版支持手动记忆、总结纸条的新增/编辑/删除；可选消息范围生成 AI 总结纸条；聊天请求会注入当前会话的手动记忆和最近 8 条总结纸条。
- 尚未完成：上游的自动定期总结、传统/向量模式切换、Embedding API 配置与语义检索、旧纸条压缩、导入导出和记忆统计。不能标记为完整复刻。
- UI 复刻仍需逐页对照上游 CSS/HTML，尤其首页桌面、聊天气泡与聊天设置；目前只是颜色基线部分接近，不能宣称像素级一致。

## UI fidelity audit: chat memory manager (2026-11-25)

The upstream chat-settings memory page was inspected in the original `index.html`, including its `rChatMemory()` renderer and `mem3-css` styles. The native Memory screen has been reshaped to follow the same information hierarchy rather than the earlier generic pair of add cards:

- compact three-value summary for note count, traditional-memory count and vector-memory count;
- a mode/status row;
- a dedicated manual note-generation section with start/end message indices;
- collapsible note rows showing date and source message range, with edit/delete actions;
- a separate collapsible traditional-memory group;
- search across both note and traditional-memory entries;
- a confirmation step before deleting a memory.

The screen now uses the app theme's light/dark surfaces and restrained grouped rows instead of hard-coded white cards and oversized headings. The native screen intentionally does not display non-functional controls for upstream features that are not yet ported.

Still missing from upstream memory parity: automatic summary toggle and interval, per-chat context-count override, vector-memory mode and Embedding API configuration, vectorization/rebuild actions, old-note compression, custom summary instructions, and import/export of traditional memories. These must be implemented with real persistence and API behavior before being shown as working controls.

Visual parity is still a work in progress across the entire app. The upstream global visual baseline is white/black with muted secondary text, 14–15px system typography, low-contrast grouped surfaces, thin separators, and compact 16–22px corner radii depending on component. The home desktop's exact card geometry, original icon placement, chat bubbles, settings rows, and page transitions still require systematic comparison and correction. This is not a claim of pixel-level parity.


## Per-chat memory controls (2026-11-25)

- Added a per-conversation context message limit (1–1000, blank means full history), persisted locally and applied to the actual chat request.
- Added per-conversation custom memory-summary instructions, persisted locally and appended to the summary-generation system prompt.
- These settings are keyed by the stable conversation ID, so they do not bleed between different chats/personas.
- Memory injection includes all manually maintained traditional memories and the latest eight generated note entries; older notes can be recalled one per day when relevant.
- Automatic summary scheduling and safe old-note compression are implemented. Vector memory/embeddings and traditional-memory import/export remain unimplemented.


## Automatic per-chat memory notes (2026-11-25)

- Added an opt-in automatic summary switch for each conversation.
- Added a configurable trigger interval of 20–2000 newly exchanged messages (default 200), matching the upstream range.
- After a successful assistant reply, the app reads the persisted conversation history and generates a memory note when the threshold is reached.
- The generated note records its source message range and is saved to the existing Room memory table; the last summarized message count is tracked per conversation in local preferences.
- The same per-chat custom summary instructions are applied to automatic summaries.
- Summary generation is best-effort: a summary API failure does not invalidate or block the already-saved chat reply, and the threshold is retried on a later successful reply.
- The feature is opt-in and disabled by default. It uses the currently configured chat model/API and may incur an additional API request when triggered.


## Old memory compression and daily recall (2026-11-25)

- The memory screen offers compression once at least three generated notes are older than the latest eight.
- Compression combines any prior merged note with the older notes, asks the configured chat model to produce an early-memory summary, then atomically writes the replacement and deletes the replaced source notes in one Room transaction.
- The latest eight ordinary notes remain untouched and continue to be injected normally.
- Older notes, including a merged early-memory note, are not bulk-injected. One older note is selected deterministically per day and supplied as a clearly labeled, dated recollection so the model does not present it as a recent event.
- If the compression request fails or returns empty content, source notes remain intact.


## Chat screen parity adjustments (2026-11-25)

- Reduced message-list vertical spacing to the upstream 4 dp rhythm and aligned list insets to 14 dp horizontal / 12 dp vertical.
- Added the upstream first-message-in-group top separation while retaining grouped bubble corner treatment.
- Remaining parity work includes exact toolbar actions, profile/avatar handling, search UI, attachments, and full interaction coverage.


## Native settings screen (2026-11-25)

- Replaced the settings placeholder route with a working native settings screen.
- Added editable API base URL, model ID, API key, and custom JSON headers.
- API keys continue to be encrypted through the existing Android Keystore-backed DataStore implementation.
- The settings page links to the existing legacy backup inspection/import flow.
- OpenCode-compatible endpoints continue to receive the automatically generated per-conversation session header from the existing header resolver.
- This is the first native settings slice; appearance, quota, and other upstream settings sections still require parity work.
