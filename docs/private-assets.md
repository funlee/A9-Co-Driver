# 本地私有资源

`private/` 不受本项目 MIT 许可覆盖，不应推送到 GitHub 或上传到公开附件。仅在具有所需使用权的本地环境使用。

```text
private/
  assets/data/          # 路线、地图规则、识别字库、表单、来源记录
  assets/lua/           # 提取及改编的旧 Lua 模块
  analysis/            # 原始逆向分析输出
  source-apks/         # 原参考 APK
  test-code/           # 依赖实际资源的本地综合回归测试
  test-fixtures/core/  # 实际像素与图像样本
  test-fixtures/android/ # 截图 OCR 回放样本
  device-tests/        # 测试日志、截图、设备验证记录
  dist/                # 本地完整 APK
  docs/                # 本机历史说明与交接记录
  dependencies/        # 旧版依赖缓存；新的构建从 Maven 解析
  build/               # 显式私有构建的产物
  build-archive/       # 整理前的旧构建缓存
```

私有构建使用本地资源目录和公开的自编代码共同组装，不把提取内容复制回公开源码路径：

```sh
./gradlew -PwithPrivateAssets=true :app:assembleDebug :app:lintDebug
sh tools/check-core.sh --private
```

输出为 `private/build/app/outputs/apk/debug/app-debug.apk`，原包名 `dev.funlee.a9local`。缺失必要私有文件时，Gradle 直接报告缺失文件并停止。默认公开构建使用完全不同的输出目录，避免误取旧的完整 APK。

`tools/prepare_assets.py` 仅处理已有本地分析文件，不下载原 APK；已有私有资源时拒绝覆盖，避免丢失后续修复。私有资料不随 clone 自动获得；不得将源码展示版误称为可自动比赛的完整发行版。
