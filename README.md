# OneMate

[English](README.en.md)

OneMate 是一个面向三星键盘的 LSPosed/libxposed 模块。它主要用于把新版三星键盘缺失的 **Text editing / 文本编辑** 工具补回工具栏，并提供隐藏键盘设置项、工具栏红点控制、三星键盘配置保存与恢复等辅助功能。

> 当前项目默认面向 `com.samsung.android.honeyboard`，使用 libxposed API 102。

## 功能

- **补回 Text editing**
  - 在三星键盘工具栏加入旧版 `Text editing` 入口。
  - 提供光标移动、选择、全选、剪切、复制、粘贴、回车、退格等操作。
  - 尽量复刻旧版三星键盘的文本编辑面板布局和图标风格。

- **隐藏项开关**
  - 在 OneMate 内提供“隐藏项”页面，集中管理三星键盘中受地区、机型、Knox、AI 服务、硬件能力等条件影响的设置项。
  - 已经在当前运行环境中正常开启的项目会归类为“无需 hook”，其余项目归类为“可强开”。

- **工具栏红点控制**
  - 可禁用三星键盘工具栏 BeeItem 的新功能红点/徽标。

- **三星键盘配置存档**
  - 可保存与恢复 OneMate 模块开关，以及三星键盘自身配置。
  - 适合保存模糊音、键盘设置、实验项状态等配置。

- **应用界面**
  - 使用 Miuix 风格界面。
  - 主页提供工作状态卡片、三星键盘强制重启按钮和测试输入框。
  - 设置页提供主题设置、关于、配置保存与恢复。

## 要求

- Android API 36+。
- 三星键盘包名：`com.samsung.android.honeyboard`。
- 支持 libxposed API 102 的 LSPosed/兼容框架。
- 在 LSPosed 中启用 OneMate，并将作用域勾选为三星键盘。
- Root 不是 Text editing 的必要条件，但以下功能需要 root：
  - 强制重启三星键盘。
  - 保存/恢复三星键盘自身配置。

## 使用

1. 安装 OneMate APK。
2. 在 LSPosed 中启用模块，并确认作用域包含三星键盘。
3. 打开 OneMate，按需启用 `Text editing`、红点控制和隐藏项。
4. 重启三星键盘，或在 OneMate 主页点击“强制重启三星键盘”。
5. 点击主页测试输入框，确认键盘重新拉起后功能生效。

如果开启隐藏项或保存/恢复配置后没有立即生效，先强制重启三星键盘；仍无效时再清除三星键盘数据并恢复已保存配置。

## 构建

项目使用 Kotlin、Android Gradle Plugin、Compose、Miuix 和 libxposed API。

需要安装：

- JDK 17。
- Android SDK Platform `android-37.0`。
- Android Build Tools `37.0.0`。

常用命令：

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --no-configuration-cache
```

构建 release：

```bash
./gradlew :app:assembleRelease --no-configuration-cache
```

本地 release 如果没有提供签名环境变量，会生成未签名 APK。

## 发布

项目包含 GitHub Actions tag release workflow。推送 `v*` tag 后会自动构建 release APK 并上传到 GitHub Release。

需要在 GitHub Secrets 配置：

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

生成 keystore base64：

```bash
base64 -i keystore.jks | tr -d '\n' | pbcopy
```

打 tag 发布：

```bash
git tag v1.0.0
git push origin v1.0.0
```

不要提交 keystore 文件。仓库已忽略 `*.jks` 和 `*.keystore`。

## 注意

- 三星键盘内部类和资源存在混淆，不同版本可能需要适配。
- 配置保存/恢复会通过 root 读写三星键盘数据目录，建议只在同设备、同包名、相近版本之间恢复。
- 如果键盘启动后异常退出，先关闭模块、清除三星键盘数据，再逐项重新开启功能定位问题。
