# Agent Instructions

这是一个个人维护的 Android 项目。用户主要负责需求、设计、评审和验收，Agent 负责阅读代码、实现功能、必要的文档同步和基本验证。

## 协作方式

- 优先直接解决问题，不要每次先输出很长的计划。
- 需求存在小范围空白时，可以做合理假设，并在结果里说明。
- 改动尽量小步进行，避免一次性做大重构。
- 优先复用现有结构、代码风格和交互方式，不随意引入复杂架构。
- 如果发现更大的结构问题，先简要说明，再继续当前任务。

## Git 和提交流程

- 默认不要自动创建分支。
- 默认不要自动创建 PR。
- 默认先让用户 review 本地改动，再决定是否提交。
- 不要把与当前任务无关的文件一起提交。
- 如果工作区里有其他改动，提交时只处理当前任务相关文件。

## 输出结果

输出结果时尽量包含：

- 改了什么
- 为什么这样改
- 有什么风险或限制
- 做了哪些验证
- 用户应该重点 review 哪些文件

## 文档处理

- 不要每次都要求用户先修改文档。
- 只有当功能说明、结构或任务状态明显变化时，再顺手更新相关文档。
- 默认优先维护 README 和当前专题下已有的核心文档；不要随意新建平行文档。

## 当前 WorkManager 文档索引

`workmanager-auto-flow/` 已作为独立工具目录管理，根仓库通过 `.gitignore` 忽略该目录；相关脚本、日志、APK 资源和专题文档后续在该目录自己的 git 仓库中维护。

当前 WorkManager 排查链路的核心文档位于：

```text
workmanager-auto-flow/docs/
```

阅读和维护时按下面分工理解：

- `AUTO_TEST_DESIGN.md`
  - 自动化测试流程设计文档。
  - 修改 `workmanager_auto_flow.py` 的阶段、参数、ADB 动作、清理流程或验收标准时，优先同步这里。
- `SCRIPT_CONVENTIONS.md`
  - WorkManager 脚本执行日志规范。
  - 修改脚本日志格式、标题层级、命令输出、`exit_code` 语义或产物命名规则时，优先同步这里。
- `LOG_DESIGN.md`
  - 自动化日志、App trace 和合并时间线的设计文档。
  - 修改 `workmanager_flow_check.py`、trace 事件格式或合并规则时，优先同步这里。
- `STAGE_FINDINGS.md`
  - WorkManager 排查阶段性结论和实验记录。
  - 新增实验、更新结论、记录验证结果或调整后续排查方向时，优先同步这里。

文档更新原则：

- 脚本实现类规则优先落到 `SCRIPT_CONVENTIONS.md`，`AGENTS.md` 只保留执行入口和协作约定。
- 流程设计变化优先落到 `AUTO_TEST_DESIGN.md`。
- 日志合并和分析视图变化优先落到 `LOG_DESIGN.md`。
- 实验事实、观察结果、阶段结论优先落到 `STAGE_FINDINGS.md`。
- 如果只是一次临时测试运行，不默认更新文档；只有结论、规则或流程发生变化时再更新。

## 小功能开发输入习惯

用户通常会直接描述：

- 目标
- 交互
- 不做什么
- 验收标准

Agent 可以直接基于这些信息开始实现。

## WorkManager 测试脚本运行方式

WorkManager 自动化流程脚本位于：

```bash
workmanager-auto-flow/scripts/workmanager_auto_flow.py
```

当前项目中，APK Manifest 静态解析依赖 `androguard`。本机 `androguard` 安装在 pyenv 的 Python 3.13 环境中：

```bash
/Users/aschu/.pyenv/versions/3.13.0/bin/python
```

不要直接用系统或 Xcode 的 `python3` 运行该脚本，因为当前 `python3` 可能指向：

```bash
/Applications/Xcode.app/Contents/Developer/usr/bin/python3
```

这个环境没有安装 `androguard`，会导致 `workmanager_check_report.py` 的静态 APK / Manifest 检查被跳过，并且不会生成 `workmanager_manifest_*.xml`。

推荐运行方式：

```bash
python workmanager-auto-flow/scripts/workmanager_auto_flow.py --foreground-minutes 0.1667 --background-minutes 0.1667
```

也可以双击 `workmanager-auto-flow/run_workmanager_flow.command`，按提示选择包名、前台运行时间和后台运行时间；脚本会先执行 `workmanager_auto_flow.py`，成功后再执行 `workmanager_flow_check.py` 生成时间线。

如需在自动化流程开始前重新安装指定 APK，可追加 `--apk`：

```bash
python workmanager-auto-flow/scripts/workmanager_auto_flow.py --pkg com.android.bbkmusic --apk workmanager-auto-flow/apk_resources/bbkmusic_base_main_process_2026-07-06.apk --foreground-minutes 20 --background-minutes 20
```

或者显式指定 pyenv Python：

```bash
/Users/aschu/.pyenv/versions/3.13.0/bin/python workmanager-auto-flow/scripts/workmanager_auto_flow.py --foreground-minutes 0.1667 --background-minutes 0.1667
```

其中 `0.1667` 分钟约等于 10 秒。上面的命令表示：

- 前台观察 10 秒
- 后台观察 10 秒

当前固定周期采样已关闭；前台/后台观察阶段只等待指定时长，不再按检查间隔主动采样。

脚本运行后，日志默认输出到：

```bash
workmanager-auto-flow/logs/
```

一次完整成功的自动化流程通常会生成：

- `workmanager_auto_flow_*.log`
- `workmanager_check_*.log`
- `workmanager_trace_*.txt`
- `workmanager_manifest_*.xml`

如果缺少 `workmanager_manifest_*.xml`，优先检查是否用了错误的 Python 环境。
