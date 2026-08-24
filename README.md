# Speed Chromatic Aberration

A Fabric client-side mod that renders a chromatic aberration post-processing effect on the player camera. By default, the effect intensity scales in real time with camera movement speed — the faster you move, the stronger the aberration. You can also switch to a fixed intensity in the config.

## Features & Configuration

Open the mod config via Mod Menu:

| Setting | Type | Default | Description |
| --- | --- | --- | --- |
| Aberration Intensity | Slider 0%–200% | 100% | Intensity multiplier; applies immediately |
| Disable Center Safe Zone | Toggle | Off | When on, the crosshair center area also renders aberration |
| Render When Stationary | Toggle | Off | When on, a 35% baseline intensity is applied even while standing still |
| Disable Speed-Based Scaling | Toggle | Off | When on, intensity is fixed by the slider and no longer affected by movement speed |

Configuration is stored in `config/chromaticaberration.json`. The mod runs fine without Mod Menu. Debug: press F8 in-game to cycle shader debug modes.

## Shader Pack Avoidance

The mod detects active shader packs via the Iris official API (reflection-based, no hard dependency). When an Iris shader pack is enabled, the mod automatically skips post-processing rendering to avoid conflicting with the shader pipeline; rendering resumes automatically when shaders are disabled.

⚠️ Always delete the old jar from `mods/` before deploying — Fabric Loader will refuse to start if two jars share the same Mod ID.

Keep walking forward, don't look back — where the green smoke rises, that's the extraction point.
