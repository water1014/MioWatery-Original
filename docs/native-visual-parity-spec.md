# Native Android visual parity specification

## Product decision

The native Android app reproduces the deployed MioWatery Pages experience. It is a native Kotlin + Jetpack Compose implementation, not a WebView and not a redesign. The visual source of truth is the user's deployed MioWatery page and its current source snapshot.

Target form factor: vivo S19 / Android 16. Platform-owned permission dialogs and system bars may differ, but page composition, proportions, colors, typography hierarchy, icon artwork, card treatment, navigation, and interaction states should follow the source.

## Source design tokens

Extracted from the deployed source `:root` and `[data-theme=dark]`:

| Token | Light | Dark |
| --- | --- | --- |
| Background `--bg` | `#FFFFFF` | `#000000` |
| Surface `--p` | `#F3F4F5` | `#303030` |
| Primary text `--ink` | `#0F1419` | `#F2F2F2` |
| Secondary text `--sub` | `#6B7480` | `#71767B` |
| Divider `--line` | `#EAECEE` | `#2F3336` |
| AI bubble `--ai` | `#ECEEF0` | `#303030` |
| User bubble `--me` | `#111111` | `#FFFFFF` |
| User bubble text `--me-ink` | `#FFFFFF` | `#000000` |
| Field `--fld` | `#FFFFFF` | `#000000` |
| Avatar surface `--av` | `#E3E5E8` | `#2A2A2A` |
| Secondary text `--t2` | `#333333` | `#DDDDDD` |
| Error `--err` | `#D93636` | `#FF7B7B` |

Typography source stack: `-apple-system, PingFang SC, Noto Sans SC, Hiragino Sans, Microsoft YaHei, sans-serif`. Preserve each component's source font size and weight; do not apply one generic Material typography scale to every page.

## Home screen: four actual horizontal pages

The deployed source contains four `.hp-slide` pages. The native implementation must retain all four in this exact order. Do not collapse them into a generic launcher or reorder the widgets.

### Page 1: clock, image/weather and messages

- Full-width clock and date.
- Full-width widget area below the clock.
- Combined memo-image and weather card with editable title/image and weather city/temperature/condition.
- Recent Messages card with a See all action.
- Preserve the source's page padding, card placement, and scroll behavior.

### Page 2: four shortcuts, recent searches and quote/photo widget

- Four shortcut buttons in the left/top grid: 日记, iPhone, 桌面美化, 音乐.
- Recent Searches widget with three rows and Clear All action.
- Quote/photo widget with editable quote and three image slots.
- Preserve the source's two-column `.page2-layout` composition; do not replace with a full-width app grid.

### Page 3: music player, iMessage preview and four shortcuts

- Music card at the top with current cover, title, artist, play control, open-music control, and recent-play list.
- iMessage preview in the left column, with editable conversation preview and compose decoration.
- Right-side four shortcuts: 情侣空间, 共读, 线下, 讯息.
- Preserve the music-first order and the original two-column composition.

### Page 4: app shortcuts and music artwork

- Eight shortcuts: 钱包, MCP, 预设, 生活, IF线, 心迹, 仓库, YouTube.
- Music artwork/player card below or alongside according to the source responsive layout.
- Keep this as the fourth swipe page; do not move its shortcuts to page 2 or the drawer.

### Home navigation

- Four horizontal pages, horizontal swipe, original page indicator/dots.
- Four fixed dock entries: 聊天, 世界书, 联系人设定, 设置.
- No generic Material Navigation Drawer or extra home top app bar unless it is explicitly present in the source.
- Match safe-area spacing and dock position to the source.

## Icons and assets

- Original home shortcut icons are inline SVG in the deployed HTML. Reuse the exact SVG path data, viewBox, stroke width, caps, joins, and fill behavior when converting to Android VectorDrawable.
- Do not substitute Material icons, emoji, or approximate line art.
- Keep app artwork and app launcher icon distinct; do not stretch the launcher icon into shortcut artwork.
- Preserve source image assets and their aspect ratios where available.

## Shared geometry and responsive behavior

- Main view content max width: 640dp where the source uses a 640px cap.
- Home content max width: 560dp where the source uses a 560px cap.
- Home outer spacing, component-specific radii, shadows, and grid gaps must be extracted from the corresponding CSS selectors rather than normalized to one shared card style.
- The source uses compact mobile-first layouts; verify on the vivo S19 viewport and density.
- Respect Android display cutout and safe-area insets without covering the page title or changing the source's content hierarchy.

## Acceptance criteria

1. Capture reference screenshots from the deployed Pages version and native screenshots at the same viewport/density for all four home pages in light and dark themes.
2. Verify page order, dock, swipe gestures, page indicator, widget taps and shortcut navigation.
3. Compare colors, geometry, icon SVG paths, typography, gradients, shadows, and empty/active states.
4. Audit every feature page independently; do not infer that a page is visually complete because its route exists.
5. Keep all work isolated to branch `native-android`; do not change the web source or upstream mainline.
