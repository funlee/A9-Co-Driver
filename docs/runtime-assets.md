# 运行资源与来源

默认构建直接使用仓库内 `app/src/main/assets/` 的文件，不依赖本机私有目录，也不需要下载或解包原参考 APK。

| 资源 | 用途 |
|---|---|
| `data/routes.txt` | 202 条地图跑法记录 |
| `data/map-rules.tsv` | 国服显示名、内部路线名及识别关键词映射 |
| `data/progress.lib` | 58 个数字字形模板；这是文本字库，不是二进制库 |
| `data/forms.json` | 七类任务的配置项、选项与默认值 |
| `data/provenance.json` | 参考 APK 摘要、来源说明、发布时资源摘要和验证范围 |
| `lua/shared.lua`、`utilities.lua`、`workflows.lua` | 适配后的共享状态、工具和任务运行逻辑 |
| `lua/multiplayer_up.lua`、`multiplayer_down.lua`、`cup.lua`、`special.lua`、`club.lua`、`career.lua`、`daily.lua` | 适配后的各赛事模块 |
| `lua/bootstrap.lua`、`multiplayer_current.lua` | 本项目编写的宿主适配、队列调度和现行多人赛流程 |
| `licenses/` | LuaJava、jnigen 等依赖许可原文 |

维护者于 2026-11-07 确认已获得提取及适配资源的公开再分发授权，并将这些运行资源纳入本项目开源范围，随项目采用根目录 MIT 许可。资源的提取来源继续保留，不将其表述为全部独立原创。此说明记录维护者的授权确认，不代表对原 APK 或游戏本体授予许可。

提交新资源时同步更新 `tools/audit-public.py` 的明确清单；修改已有资源时更新 `provenance.json` 对应摘要及验证说明。审核脚本检查资源是否齐全，并可比较 APK 内资源与源码是否逐字节一致。克隆者可运行 `sh tools/check-core.sh` 验证随库资源和调度逻辑。

`tools/prepare_assets.py` 保留为可选的历史转换工具：它只读取本机 `private/analysis/`，输出到 `private/assets/`，不会覆盖随库资源或参与默认构建。重新转换不是运行助手的前置步骤。
