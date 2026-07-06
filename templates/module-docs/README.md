# 模块文档模板

这组模板用于在 `aimusic` 中快速新建一个独立调研模块，或在现有模块内新建一个独立子能力的文档骨架。

建议使用顺序：

1. 先阅读根目录 [RESEARCH_DEVELOPMENT_GUIDELINES.md](../../RESEARCH_DEVELOPMENT_GUIDELINES.md)
2. 按需要复制下面的模板文件
3. 先补 `PRD / TASKS / ARCHITECTURE`
4. 形成稳定入口后，再补 `INTEGRATION / DESIGN`

使用原则：

- 模板用于提效，不用于增加流程负担
- 不适用的章节可以直接删除
- 当前阶段不需要的文档可以后补
- 小型调研优先保留最小闭环所需内容
- 当前文档优先描述“现在做到哪、边界在哪、怎么接入”，不要写成纯实施流水账

推荐复制后的命名方式：

- 模块级文档：
  - `docs/PRD.md`
  - `docs/TASKS.md`
  - `docs/ARCHITECTURE.md`
  - `docs/INTEGRATION.md`
  - `docs/DESIGN.md`
- 子能力文档：
  - `docs/FEATURE_PRD.md`
  - `docs/FEATURE_TASKS.md`
  - `docs/FEATURE_ARCHITECTURE.md`
  - `docs/FEATURE_INTEGRATION.md`
  - `docs/FEATURE_DESIGN.md`

模板目标：

- 减少重复组织文档结构的时间
- 统一调研模块的输出风格
- 让后续接手的人更快找到入口、限制和接入方式
