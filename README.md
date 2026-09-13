# GestureDot / 复刻球

一个以悬浮球主动触发录制手势的 Android 工具。当前版本完成了第一个工程闭环：启用无障碍服务后显示悬浮球，单击悬浮球执行默认向右滑动。

[![一键下载 APK](https://img.shields.io/badge/Download-APK-6750A4?style=for-the-badge&logo=android)](https://github.com/star132-bot/MKControlPhone/releases/download/v0.1.2-alpha/GestureDot-v0.1.2-alpha-debug.apk)
[![Android CI](https://github.com/star132-bot/MKControlPhone/actions/workflows/android.yml/badge.svg)](https://github.com/star132-bot/MKControlPhone/actions/workflows/android.yml)

> 当前版本：`v0.1.2-alpha`（Debug 测试版，Android 8.0 及以上）。如果按钮无法下载，请前往 [版本发布页面](https://github.com/star132-bot/MKControlPhone/releases/tag/v0.1.2-alpha)。首次打开会自动显示权限用途说明；同意后需在系统无障碍设置中手动开启“复刻球手势服务”。

详细产品和技术定义见 [docs/SPEC.md](docs/SPEC.md)。
权限、易用性验收与自动发布流程见 [docs/RELEASE_AND_UX.md](docs/RELEASE_AND_UX.md)。

## 当前能力

- Compose 设置首页与无障碍服务状态检查
- 首次启动自动显示权限用途说明与明确同意流程
- 无需 `SYSTEM_ALERT_WINDOW` 的 Accessibility Overlay 悬浮球
- 悬浮球拖动与单击/双击/三击识别
- 标准化坐标动作模型
- `dispatchGesture()` 默认向右滑动
- DataStore 保存悬浮球显示设置

双击快捷盘、真实最近动作和录制按钮目前显示为下一阶段占位能力。

## 构建环境

- Android Studio 2025.1.4 或更新稳定版
- JDK 17
- Android SDK 36
- Gradle 9.6（Wrapper）

打开项目后等待 Gradle Sync，连接 Android 8.0 以上真机运行 `app`。首次启动会自动显示权限说明；点击“同意并前往设置”后，在系统设置中开启“复刻球手势服务”。

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## 隐私基线

当前应用没有网络权限，AccessibilityService 设置为 `canRetrieveWindowContent=false`。应用只在用户主动触发时派发手势。
