# MioWatery Android 原生版

这是 MioWatery 的独立 Android 原生工程，位于 `native-android` 分支的 `android/` 目录。

## 当前状态

目前仅完成第一版工程骨架：
- Kotlin + Jetpack Compose。
- Material 3 浅色 / 深色主题基础。
- 原生首页与导航抽屉占位。
- Gradle Version Catalog。
- Room、DataStore、OkHttp、Coil、Media3 等基础依赖入口。

**这不是功能完成版。** 首页卡片和抽屉目前是迁移入口占位，聊天、记忆、生图、音乐等业务尚未迁入。没有使用 WebView 承载页面。

## 环境

- JDK 17
- Android SDK Platform 36
- Gradle 8.13
- Android Gradle Plugin 8.13.2

## 本地构建

在本目录执行：

```bash
gradle assembleDebug
```

首次构建需要下载 Android Gradle Plugin 与依赖，并配置 Android SDK。

## 包名

`com.water1014.miowatery`

## 数据安全

现阶段不读取、不覆盖旧版浏览器数据。Room 数据库与旧版 IndexedDB 迁移器尚未实现。


## 迁移资料

- [总迁移计划](../docs/native-migration-plan.md)
- [功能入口清单](../docs/native-feature-inventory.md)
- [页面与设置子页面清单](../docs/native-page-inventory.md)
- [旧版数据映射](../docs/native-data-map.md)
- [AI 引擎兼容规格](../docs/native-ai-engine-spec.md)
