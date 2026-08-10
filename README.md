# Speed Chromatic Aberration（速度色差）

Fabric 客户端 Mod：在玩家相机视角渲染色差（Chromatic Aberration）后处理效果。效果强度默认随相机移动速度实时变化——速度越快，色差越明显；也可在配置里改为固定强度。



## 功能与配置

安装 Mod Menu 后打开本 Mod 配置：

| 配置项 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| 色差强度 | 滑轨 0%–200% | 100% | 强度倍率，实时生效 |
| 禁用中心半屏无效果区 | 开关 | 关 | 开启后准星中心区域也渲染色差 |
| 静止时也渲染色差 | 开关 | 关 | 开启后不动也有 35% 基线强度 |
| 禁用速度改变效果强度 | 开关 | 关 | 开启后强度只由滑轨决定，不再随速度变化 |

配置存于 `config/chromaticaberration.json`。未安装 Mod Menu 不影响运行。调试：游戏内按 F8 循环切换着色器调试模式。

## 光影避让

通过 Iris 官方 API（反射调用，无硬依赖）检测光影包：Iris 光影启用时，本 Mod 自动放弃后处理渲染，避免与光影管线抢最终画面；关闭光影后自动恢复。

## 构建

每个版本先切换参数，再用对应 Gradle/JDK 构建：

```bash
./switch-version.sh <mc_version>
gradle clean build
```

- `1.20.1`–`1.21.1`：Loom 1.6，Gradle 8.8，JDK 17/21 工具链。
- `1.21.3`–`1.21.11`：Loom 1.15.5，Gradle 9.6.1，JDK 21。
- `26.1.2`：Mojang 官方名、无混淆，Loom 1.15.5，Gradle 9.6.1，JDK 25。

产物在 `build/libs/`；发布副本放入 `交付/`。每个版本的 Gradle 编译文件快照放入 `Geadle/<mc_version>/`。

## 版本命名规则

`gradle.properties` 中的 `debug_version` 控制产物名：

| 场景 | debug_version | 产物 |
| --- | --- | --- |
| 正式发布 | （留空） | `speed-chromatic-aberration-1.0.0.jar` |
| 大修 | 递增主版本，如 `2.0.0` | `speed-chromatic-aberration-1.0.0-debug2.0.0.jar` |
| 小修 | 递增次版本，如 `1.2.0` | `speed-chromatic-aberration-1.0.0-debug1.2.0.jar` |

多版本产物会追加 `+mc<version>`，例如 `speed-chromatic-aberration-1.0.0-debug4.0.0+mc1.21.8.jar`。

⚠️ 部署时必须先删除 mods 里的旧 jar——同名 Mod ID 的两个 jar 会让 Fabric Loader 拒绝启动。

The world may be better without me...
So I want to continue to live.