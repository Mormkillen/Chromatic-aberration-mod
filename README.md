# Speed Chromatic Aberration

For mod download, please go to：https://modrinth.com/project/r0tit6tM

A Fabric client-side mod that renders a chromatic aberration post-processing effect from the player's camera perspective. By default, the effect intensity changes in real time with camera movement speed — the faster you move, the more pronounced the aberration. A fixed intensity can also be set in the configuration.

Actively maintained.

## Features & Configuration

Open this mod's configuration after installing Mod Menu:

| Option | Type | Default | Description |
| --- | --- | --- | --- |
| Aberration Intensity | Slider 0%–200% | 100% | Intensity multiplier, takes effect immediately |
| Disable Center Safe Zone | Toggle | Off | When enabled, the reticle center area also renders chromatic aberration |
| Render at Rest | Toggle | Off | When enabled, a 35% baseline intensity is applied even when standing still |
| Disable Speed-Based Scaling | Toggle | Off | When enabled, intensity is controlled solely by the slider and no longer varies with speed |

The config file is stored at `config/chromaticaberration.json`. The mod runs normally even if Mod Menu is not installed. Debug: press F8 in-game to cycle through shader debug modes.

## Shader Pack Compatibility

This mod detects shader packs via the Iris official API (reflection call, no hard dependency). When an Iris shader pack is active, this mod automatically skips post-processing rendering to avoid conflicts with the shader pipeline; rendering resumes automatically when shaders are disabled.

⚠️ When deploying, you must first delete the old JAR in the `mods` folder — having two JARs with the same Mod ID will cause the Fabric Loader to refuse to start.
