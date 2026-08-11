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

## Building

Switch versions first, then build with the matching Gradle / JDK:

```bash
./switch-version.sh <mc_version>
gradle clean build
```

- `1.20.1`–`1.21.1`: Loom 1.6, Gradle 8.8, JDK 17/21 toolchain.
- `1.21.3`–`1.21.11`: Loom 1.15.5, Gradle 9.6.1, JDK 21.
- `26.1.2`: Mojang official names, no obfuscation; Loom 1.15.5, Gradle 9.6.1, JDK 25.

Artifacts land in `build/libs/`; release copies go into `交付/`. Each version's Gradle build-file snapshot goes into `Geadle/<mc_version>/`.

## Version Naming

The `debug_version` in `gradle.properties` controls the artifact name:

| Scenario | debug_version | Artifact |
| --- | --- | --- |
| Official release | (leave empty) | `speed-chromatic-aberration-1.0.0.jar` |
| Major revision | bump the major part, e.g. `2.0.0` | `speed-chromatic-aberration-1.0.0-debug2.0.0.jar` |
| Minor revision | bump the minor part, e.g. `1.2.0` | `speed-chromatic-aberration-1.0.0-debug1.2.0.jar` |

Multi-version artifacts append `+mc<version>`, e.g. `speed-chromatic-aberration-1.0.0-debug4.0.0+mc1.21.8.jar`.

⚠️ Always delete the old jar from `mods/` before deploying — Fabric Loader will refuse to start if two jars share the same Mod ID.

Keep walking forward, don't look back — where the green smoke rises, that's the extraction point.
