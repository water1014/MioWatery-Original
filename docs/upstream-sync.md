# Upstream sync log

## Source of truth
- Upstream: https://github.com/water649/MioWatery
- User web fork: https://github.com/water1014/MioWatery
- Native Android app: https://github.com/water1014/MioWatery-Original
- Native development branch: `native-android`
- Checked: 2026-11-25
- Upstream main checked commit: `497609853f1ca8c900fabb293b4dfb7cf09aa6fb`

## 2026-11-25 upstream changes

Upstream changes since the common ancestor of the web fork:

1. Desktop customizer "More" section adds desktop theme pack import/export.
   - Includes wallpaper, app icons, built-in/custom widgets, widget order and placement.
   - JSON file export or copyable JSON text.
   - Import from file or pasted JSON.
2. Built-in desktop widget hiding is hardened against CSS selectors with higher specificity.
3. README documents theme-pack sharing and warns that custom widgets contain executable code.

## Web fork status

- `water1014/MioWatery:main` has user-owned Chinese settings translations.
- Upstream and fork are divergent. GitHub reports a merge conflict on the upstream sync PR.
- Do not force-push or replace the fork's `index.html`.
- Preserve the Chinese settings section and manually reconcile upstream desktop-customizer changes before updating the fork's `main`.
- The upstream sync PR was left open for visibility; do not merge until the conflict is resolved.

## Native Android port status

- All native work stays on `native-android`; web HTML/JavaScript is never merged into the native app.
- Implemented a validated reader for upstream `xsj-desktop-theme` v1 packs. It extracts safe HTTPS/data-image assets, icon overrides, supported clock/text/image widgets, hidden IDs and placement metadata.
- Implemented native file import, preview, explicit apply confirmation, and safe JSON export. Export strips unsupported web custom-widget code rather than copying it into the native package.
- Applied desktop wallpaper and icon overrides are persisted in the app-private files directory using atomic replacement and loaded off the main thread.
- Supported native clock/text/image widgets render on the home page; imported wallpaper and app icons also render there.
- Worldbook now has an explicit back action, supports selecting the target contact for persona-scoped entries, and merges edited fields into the original raw JSON so unknown legacy metadata survives.
- Persona avatar picker attempts to retain read permission so selected images remain available after process restart.
- Not yet ported: exact desktop drag/drop placement and the full hidden built-in widget map. Placement/hidden metadata is preserved in the pack, but current native home layout does not yet reproduce the web grid positions.
- Do not execute imported HTML, CSS or JavaScript. Native custom widgets use only explicitly supported declarative types with bounded fields.
- The upstream CSS specificity fix is a behavior requirement, not code to copy: native widget visibility must be derived from one authoritative state so hidden widgets cannot reappear from layout recomposition.

## Ongoing rule

Before each substantial native feature batch, compare upstream `main` with the last checked SHA, summarize new user-facing features and bug fixes, then port relevant behavior natively. Never overwrite the user's web-fork customizations as a shortcut.
