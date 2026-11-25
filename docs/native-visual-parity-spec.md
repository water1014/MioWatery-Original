# Native Android visual parity specification

## Product decision

The native Android app must reproduce the existing MioWatery Pages experience, not invent a new visual system inspired by it. The original `main/index.html` is the visual source of truth. Implement the interface with native Jetpack Compose components; do not embed the website in a WebView.

The goal is close visual and interaction parity on the vivo S19 form factor. Platform differences such as system font rasterization and status/navigation bars may remain, but layout, proportions, colors, typography hierarchy, icon artwork, card treatment, navigation behavior, and interaction states must follow the source.

## Source design tokens

Extracted from the original `:root` CSS:

| Token | Light value |
| --- | --- |
| Background `--bg` | `#FFFFFF` |
| Surface `--p` | `#F3F4F5` |
| Primary text `--ink` | `#0F1419` |
| Secondary text `--sub` | `#6B7480` |
| Divider `--line` | `#EAECEE` |
| AI bubble `--ai` | `#ECEEF0` |
| User bubble `--me` | `#111111` |
| User bubble text `--me-ink` | `#FFFFFF` |
| Field `--fld` | `#FFFFFF` |
| Avatar surface `--av` | `#E3E5E8` |
| Error `--err` | `#D93636` |

Dark theme values must follow the original CSS `[data-theme=dark]` variables rather than Material defaults.

Typography source stack: system sans, PingFang SC, Noto Sans SC, Hiragino Sans, Microsoft YaHei. Base size 15sp with 1.5 line height.

## Shared geometry

- Main view content max width: 640dp.
- Home content max width: 560dp.
- Home horizontal outer padding: 18dp.
- Home top inset: 20dp plus system safe-area inset.
- Base input radius: 10dp.
- Main rounded surfaces use the original per-component radii; do not normalize every card to one radius.
- Original SVG line art is the source for app icons. Preserve viewBox, paths, stroke widths, caps and joins. Do not substitute Material icons or emoji.

## Home screen composition

The original home is a vertically full-screen, horizontally paged three-page experience, not a generic app launcher grid.

### Page 1: clock, daily widget, weather/photo and recent messages

- Two-column widget wrap with 14dp gaps.
- Clock spans the full width, aligned to the bottom, with 60sp light-weight time text.
- Weather/photo widget and recent-message card retain their original placement and component-specific rounded treatment.
- The page is scrollable inside the available viewport.
- Keep the original clean white background and low-contrast grey surfaces.

### Page 2: shortcuts and personal widgets

- Page content max width 560dp.
- Four-column app shortcut grid.
- Grid gap: 24dp vertical, 8dp horizontal.
- Grid padding: 8dp top/horizontal and 20dp bottom.
- Shortcut icon tile: 58dp square, 18dp corner radius, subtle 145-degree light-grey gradient and low-opacity shadow.
- Icon artwork: 29dp, original SVG paths.
- Label: 11sp, centered, 7dp spacing from icon, 1.35 line height.
- Dark mode uses the original dark gradient and light icon color.
- Recent searches and quote/photo widgets keep their own original component styling and placement.

### Page 3: calendar, chat preview and app shortcuts

- Two-column layout with calendar spanning the full width; chat preview and app shortcuts below.
- Outer layout gap: 14dp vertical and 12dp horizontal on the compact mobile layout.
- Calendar: white surface, 24dp radius, subtle shadow, 16dp vertical / 14dp horizontal inner padding.
- Calendar title: 25sp, bold; seven-column date grid; today uses dark filled circular marker in light theme.
- Chat preview: white surface, 22dp radius, subtle shadow; incoming bubbles grey and outgoing bubbles blue as in the source.
- App shortcuts on this page use 40dp icon tiles and 10sp labels.
- Dark theme uses the source-specific dark calendar/chat surfaces and inverse date marker.

## Navigation and behavior

- Keep the three home pages horizontally swipeable and show the original page indicator.
- Preserve the original home dock and safe-area spacing.
- Selecting a shortcut opens its matching feature screen.
- Back navigation, page selection, scroll position, and widget interactions must behave consistently with the original.
- Do not introduce a Material Navigation Drawer or a new top app bar on the home screen unless the source has an equivalent.

## Acceptance criteria

1. Compare native screenshots against the Pages source at the same device viewport and density. Check all three home pages in light and dark themes.
2. App icon artwork must match the original SVG paths, not merely share a similar style.
3. Card positions, spacing, widths, radii, typography, gradients and shadows must follow the source values.
4. Home horizontal swiping, page dots, widget taps and shortcut navigation must work.
5. A visual review is required before expanding this visual system to every feature screen.
6. Keep all work isolated to branch `native-android`; do not modify the web source or upstream mainline.
