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

- Keep all native work on `native-android`; do not merge web HTML/JavaScript into the native app.
- Port desktop theme packs as a validated JSON format and native configuration UI.
- Import/export should include wallpaper references, icon overrides, supported native widget definitions, hidden-widget state and placement/order.
- Do not execute imported HTML, CSS or JavaScript. Native custom widgets must use a constrained declarative schema with explicit supported widget types and bounded fields.
- Preview imported packs before applying them. Applying a pack must be an explicit user action and should preserve a restorable snapshot of the current desktop configuration.
- The upstream CSS specificity fix is a behavior requirement, not code to copy: native widget visibility must be derived from one authoritative state so hidden widgets cannot reappear from layout recomposition.

## Ongoing rule

Before each substantial native feature batch, compare upstream `main` with the last checked SHA, summarize new user-facing features and bug fixes, then port relevant behavior natively. Never overwrite the user's web-fork customizations as a shortcut.
