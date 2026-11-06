# 本地资料与可选截图检查

完整运行资源已经公开到 `app/src/main/assets/`。克隆、默认编译和自动任务均不依赖 `private/`；旧的 `-PwithPrivateAssets=true` 开关已移除，不再用于选择运行资源。

以下历史资料仍仅保存在本机，不作为源码上传：

- `private/analysis/`、`source-apks/`：逆向分析原始输出和参考 APK。
- `private/assets/`：迁移前资源备份；当前程序不会从这里加载资源。
- `private/test-code/`：迁移前测试代码备份；当前测试源码在 `core/src/test/`。
- `private/test-fixtures/`、`device-tests/`：截图、像素回放、账号相关画面及运行日志。
- `private/dist/`、`build/`、`build-archive/`、`dependencies/`：旧 APK、构建产物和依赖缓存。
- `private/docs/`：本机排查和交接资料。

默认检查不需要任何本地截图：

```sh
sh tools/check-core.sh
```

仅在本机拥有回放样本时执行以下额外检查；样本缺失会报错，不会把未执行的检查报告为通过：

```sh
sh tools/check-core.sh --local-screenshots
./gradlew -PwithLocalTestFixtures=true :app:assembleDebugAndroidTest
```

截图检查不等于实际比赛验收。不要在运行中的助手上执行 instrumentation，它会影响应用和无障碍服务。账号数据、签名密钥、游戏安装包和本地截图均不随源码发布。
