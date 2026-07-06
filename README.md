# aimusic

当前主分支保留最小 Android 工程，只包含：

- `MyApp`
- `MainActivity`

首页展示一行 `hello world`，用于保持工程可直接启动和验证。

## 当前调研文档

当前本地保留 `workmanager-auto-flow` 调研与自动化脚本，用于排查：

- Android 14 上周期任务可拉起
- Android 14 以上周期任务无法拉起

该目录已从主 Android 工程中拆出，根仓库通过 `.gitignore` 忽略，由 `workmanager-auto-flow` 目录内的独立 git 仓库单独管理。

文档入口：

- [AUTO_TEST_DESIGN](./workmanager-auto-flow/docs/AUTO_TEST_DESIGN.md)
- [SCRIPT_CONVENTIONS](./workmanager-auto-flow/docs/SCRIPT_CONVENTIONS.md)
- [LOG_DESIGN](./workmanager-auto-flow/docs/LOG_DESIGN.md)
- [STAGE_FINDINGS](./workmanager-auto-flow/docs/STAGE_FINDINGS.md)

自动化入口：

- [run_workmanager_flow.command](./workmanager-auto-flow/run_workmanager_flow.command)
