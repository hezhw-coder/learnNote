# Tasks

- [x] Task 1: 固化当前已实现范围与剩余功能清单
  - [x] SubTask 1.1: 汇总现有三组规格、项目计划与进度台账中的已实现能力，形成统一基线
  - [x] SubTask 1.2: 逐模块识别剩余未完成功能点，按 `ETL/CDM`、报表设计器、开放 API、系统/安全、测试交付分类
  - [x] SubTask 1.3: 为每类剩余功能标注优先级、依赖关系、与现有模块的集成位置

- [x] Task 2: 补齐 ETL/CDM 剩余能力规格
  - [x] SubTask 2.1: 为就诊、科室、医生、检查、药品等候选主题定义扩展顺序与最小闭环标准
  - [x] SubTask 2.2: 明确新增主题所需的 `ODS`、`CDM`、元数据、样例预览、下游复用与数据库迁移边界
  - [x] SubTask 2.3: 明确失败重试、并发控制、调度执行、能力矩阵与告警占位的实现约束
  - [x] SubTask 2.4: 补充 ETL 模块的异常处理与边界条件验证要求

- [x] Task 3: 补齐报表设计器剩余能力规格
  - [x] SubTask 3.1: 明确拖拽布局、栅格占位、组件排序与模板 JSON 结构约束
  - [x] SubTask 3.2: 明确多组件联动、复杂筛选、绑定扩展与预览/导出/调度一致性规则
  - [x] SubTask 3.3: 明确模板版本管理、发布流程、回滚策略与兼容性要求
  - [x] SubTask 3.4: 补充报表设计器模块的异常处理与边界条件验证要求

- [x] Task 4: 补齐开放 API 剩余能力规格
  - [x] SubTask 4.1: 为新增主题与现有主题补足接口路径、查询参数、分页契约、排序规则与错误码
  - [x] SubTask 4.2: 定义更细粒度 `scope`、字段脱敏与筛选权限的判定逻辑
  - [x] SubTask 4.3: 明确访问日志、限流失败、鉴权失败与契约文档同步更新要求
  - [x] SubTask 4.4: 补充开放 API 模块的异常处理与边界条件验证要求

- [x] Task 5: 补齐系统管理与安全剩余能力规格
  - [x] SubTask 5.1: 明确字典治理、系统参数分类、启停、排序、引用校验等功能需求
  - [x] SubTask 5.2: 明确菜单权限、动作权限、数据范围权限与字段权限的协同关系
  - [x] SubTask 5.3: 明确生产安全基线、密钥轮换、HTTPS/HSTS、审计与敏感参数托管要求
  - [x] SubTask 5.4: 补充系统管理与安全模块的异常处理与边界条件验证要求

- [x] Task 6: 补齐测试、交付与文档一致性规格
  - [x] SubTask 6.1: 明确前端交互测试、后端模块测试、容器级回归与人工验收清单的对应关系
  - [x] SubTask 6.2: 定义覆盖率门禁提升方向、失败制品留档与回归入口说明
  - [x] SubTask 6.3: 明确部署文档、使用手册、接口说明、交付验证记录、项目进度说明的同步更新责任
  - [x] SubTask 6.4: 生成面向所有剩余功能点的完整功能验证 checklist

- [x] Task 7: 收口并复核 `/spec` 路径一致性
  - [x] SubTask 7.1: 校对本次新增 `spec.md`、`tasks.md`、`checklist.md` 三份文档的一致性
  - [x] SubTask 7.2: 确认本次规格与既有三组已完成规格的状态描述无冲突
  - [x] SubTask 7.3: 形成“已实现范围 / 剩余能力 / 验证清单”三位一体的可追溯规格入口

# Task Dependencies

- Task 2 depends on Task 1
- Task 3 depends on Task 1
- Task 4 depends on Task 1
- Task 5 depends on Task 1
- Task 6 depends on Task 2, Task 3, Task 4, Task 5
- Task 7 depends on Task 6
