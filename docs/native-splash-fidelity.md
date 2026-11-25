# Native splash fidelity notes

The native splash is based on the original Page implementation in `index.html` under the `XSJ-SPLASH` block.

## Original composition

- Full-screen white background, ink `#101010`, ghost lettering `#e8e8e8`.
- Main hero starts at 10vw / 45vh.
- `Mio` appears first as a pale wordmark, then dark water rises through the lettering.
- The right-hand Chinese line is `文字是贫瘠的　我想你能读懂我的泪`; `watery` is aligned to the lower edge.
- English lines appear at 65vh (left) and 78vh (right).
- Rain is sparse at first, increases while the water fills the wordmark, then settles.
- Original timeline: ghost at 250ms, water/wipe at 650ms, poem at 4000ms, transition at 6800ms, fallback at 7600ms.
- A tap on the original splash skips the remaining animation and fades out quickly.

## Native implementation

`MioSplashScreen.kt` recreates the same composition and timeline in Compose, with a clipped water-reveal for the wordmark, a rain canvas, the original copy, and tap-to-skip. It is rendered as a native overlay before the home screen. This keeps the app free of a WebView-based splash.

The native animation is a visual reconstruction of the original source behavior, not a claim that the original HTML animation runtime is embedded in the app. Further pixel-level tuning should be done against screenshots from the original Page on the same device dimensions.
