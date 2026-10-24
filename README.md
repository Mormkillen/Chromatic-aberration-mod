# Chromatic aberration mod

A Fabric client-side mod that renders a chromatic aberration post-processing effect from the player's camera. By default the strength follows camera movement speed — the faster you move, the stronger the red/blue fringing. The effect can also be pinned to a fixed strength.

## Compatibility

| | |
| --- | --- |
| Minecraft | 1.21 - 1.21.1 |
| Fabric Loader | 0.15.0 or newer |
| Fabric API | required |
| Mod Menu | optional (config screen) |
| Java | 21 or newer |

## Features

- **Speed-driven strength** — fades in above walking speed and ramps up smoothly, so the effect reacts to how fast you are actually moving.
- **Clean center** — the middle of the screen stays sharp; the fringing only ramps in towards the edges, keeping the crosshair readable.
- **Fixed-strength mode** — ignore camera speed entirely and drive the effect from the slider.
- **Idle baseline** — optionally keep a 35% effect while standing still.
- **Iris aware** — steps aside while an Iris shader pack is active and resumes automatically when it is disabled.
- **Free when idle** — nothing is rendered while the strength is effectively zero.

## Configuration

Open the config from Mod Menu; the mod works without it. Settings are saved to `config/chromaticaberration.json`.

| Option | Type | Default | Description |
| --- | --- | --- | --- |
| Aberration intensity | Slider 0–200% | 100% | Strength multiplier, applied immediately. |
| Disable center safe zone | Toggle | Off | Apply the effect across the whole screen, including the crosshair area. |
| Render while idle | Toggle | Off | Keep a 35% baseline effect when you are not moving. |
| Disable speed-based intensity | Toggle | Off | Use only the slider and ignore camera speed. |

## Installation

1. Install Fabric Loader for Minecraft 1.21 - 1.21.1.
2. Put Fabric API in `mods/`; Mod Menu is optional but required for the config screen.
3. Put the mod jar in `mods/`.
4. Delete older copies of this mod first — Fabric refuses to start when two jars share a mod id.

## Building

Each branch is a complete Gradle project with the wrapper included.

```bash
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```

The jar is written to `build/libs/`.

## Iris and shader packs

Shader packs are detected through the Iris API. While a pack is active the mod leaves the final image alone, so you get neither a doubled effect nor a fight over the framebuffer.

## Debug key

Press <kbd>F8</kbd> to cycle the shader debug modes: normal, passthrough copy, forced opaque alpha, alpha visualisation. Handy when reporting a rendering issue.

## License

MIT — see [LICENSE](LICENSE).

---

> The world is beautiful, and you can't even see it.
