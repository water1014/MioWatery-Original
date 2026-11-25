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
| Home / desktop | Partial | Theme-pack wallpaper/icons/widgets/hidden IDs are supported; exact grid placement, ordering, full widget map and responsive polish remain. |
| Chat / messages | Partial | Local text chat, OpenAI-compatible API transport, streaming parser and reasoning storage exist. Attachments, multimodal payloads, robust cancellation, conversation list/search and full chat controls remain. |
| Contacts / personas | Partial | Persona list, editor, avatar selection and persona-specific chat entry exist. Full profile fields, import/export, deletion/management and parity remain. |
| Worldbook | Partial | CRUD, global/persona scope, trigger keywords and prompt injection exist. Full legacy schema parity, book-level management/import/export and advanced activation rules remain. |
| Desktop customization | Partial | Safe theme-pack import/preview/apply/export exists. Exact placement and full built-in widget compatibility remain. |
| Backup / migration | Partial | JSON/ZIP preview and confirmed import for selected core records exist. Full data-type coverage, round-trip export and upgrade preservation verification remain. |
| Diary | Partial (first native implementation) | Local create/edit/delete, date sorting and Room persistence. Search, annotations, image attachments, export and calendar integration remain. Keep the diary scope aligned with upstream; do not invent mood/favorite/lock features. |
| Music | Placeholder | Library, local media import, background playback, queue, playback controls and notification controls. |
| Couple space | Placeholder | Relationship timeline, shared records and configurable relationship start date. |
| Co-reading | Placeholder | Books, reading progress, notes and shared reading sessions. |
| Reading | Placeholder | Book library, reader, bookmarks and progress. |
| Messages | Placeholder | Message inbox, notifications and deep links; define relationship to chat conversations. |
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

- `app` and `msgs` are distinct upstream routes. Native `app` opens chat; `msgs` now has its own route and is explicitly not redirected to ChatScreen. The message inbox itself is still a placeholder.
- The native desktop app grids now use the upstream page grouping and no longer display the invented generic "应用" title, calendar page, or "常用应用" shortcut card.
- Theme-pack widgets are no longer rendered as extra cards on the first clock/weather page; they are placed below the original first app-grid page, pending exact positional mapping from the source desktop layout.
- The four fixed dock entries are excluded from the three app-grid pages to avoid duplicate icons.
- The original source also contains CSS beautification, stickers management and data backup as internal/settings/chat workflows. They should not be promoted to invented desktop shortcuts without evidence.
- Backup remains available during migration work and should ultimately be placed under the original Settings/Data flow.
- This is a navigation and grouping correction, not a claim that the listed app features are already implemented. Placeholder modules remain clearly marked as incomplete.
