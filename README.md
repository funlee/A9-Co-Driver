# A9 Copilot

A9 的本地 Android 自动化助手源码。使用屏幕识别、无障碍触控和 Lua 调度执行任务，无需助手账号，不接入原工具的授权、远程 OCR 或更新服务。

本仓库只公开自行编写的实现。从其他 APK 提取的脚本、路线、字库及其改编版本集中保留在本机 `private/`，不在本仓库的开源许可或发布范围内。**公开源码可以编译和运行独立测试；默认构建不会执行自动比赛。** 完整本地功能仍依赖自行提供且具有使用权的私有资源。

## 公开内容

- `app/src/main/java/`：Android 界面、屏幕采集、OCR、触控、前台检查和悬浮面板。
- `core/src/main/java/`：路线解释与动作调度、地图匹配、进度过滤、名次解析和任务统计。
- `app/src/main/assets/lua/bootstrap.lua`：自行编写的宿主适配与项目队列调度。
- `app/src/main/assets/lua/multiplayer_current.lua`：自行编写的世界/大师系列赛流程。
- `core/src/test/`：不依赖原 APK 资源的合成数据测试。
- `app/src/androidTest/`：截图回放测试代码；截图本身只在私有目录。

运行逻辑包括地图失败后的左划兜底、完成后切换项目、识别到无油车辆时换车、关闭已知弹窗、新车提示只允许稍后查看，以及完赛数和个人前三名率统计。这些能力的完整运行还需要私有资源，代码存在不等于全部场景已经验收。

## 构建与检查

需要 JDK 17、Android SDK Platform 36 和 Build Tools 36.0.0。设置 `JAVA_HOME`、`ANDROID_HOME`（或本地 `local.properties`）；Gradle Wrapper 会下载声明的依赖。

```sh
./gradlew :app:assembleDebug :app:lintDebug --console=plain
sh tools/check-core.sh
python3 tools/audit-public.py
```

默认输出：`build/public/app/outputs/apk/debug/app-debug.apk`，包名 `dev.funlee.a9local.source`。该源码展示版不含私有资源，不能启动比赛，也不会覆盖原本机完整助手。

需要本地完整构建时，参见 [私有资源说明](docs/private-assets.md)。仅将文件复制到私有目录不会让默认构建打包它们，必须显式指定 `-PwithPrivateAssets=true`。

## 当前验收范围

本地完整构建曾完成世界系列赛五连跑；大师系列赛累计十局分两段完成。无油换车已有本地验证。完整 30 世界 + 20 大师循环、名次识别与面板避让的持续实战、全部路线和其他赛事仍未全部验收。小米 15 未完成重新验收。

公开测试验证算法与调度；私有测试还使用本地资源和游戏截图。二者均不代替真实设备的完整比赛验收。不会把测试截图、游戏账号信息或设备日志放入公开仓库。

## 发布边界

`private/`、完整 APK、原 APK、截图、日志、签名密钥和本地工具缓存均禁止提交。提交前运行 `tools/audit-public.py`；可执行 `git config core.hooksPath .githooks` 启用提交及推送前检查。现有本地完整 APK 不作为公开 Release 附件。

## 许可证

自行编写的代码采用 [MIT](LICENSE)。外部依赖保留各自许可，见 [第三方依赖说明](THIRD_PARTY_NOTICES.md)。私有资源不因目录整理获得任何开源或再分发授权。
