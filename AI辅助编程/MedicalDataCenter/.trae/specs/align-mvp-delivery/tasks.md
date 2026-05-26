# Tasks

- [x] Task 1: 完成 MVP 偏差基线梳理与范围冻结
  - [x] SubTask 1.1: 对照 `medical-data-center-mvp-plan.md` 与 `docs/项目进度说明.md` 输出模块级偏差清单
  - [x] SubTask 1.2: 将偏差项归类为“阻断问题 / 核心缺口 / 文档不一致 / 验证缺失”
  - [x] SubTask 1.3: 冻结本轮仅允许推进的 MVP 范围，排除超范围扩展

- [x] Task 2: 修复影响主链路交付的基础问题
  - [x] SubTask 2.1: 修复 TiDB/MySQL 方言不兼容的运行时 SQL
  - [x] SubTask 2.2: 校正 Docker、Nginx、环境配置与真实运行方式不一致的问题
  - [x] SubTask 2.3: 校正接口说明、联调脚本、测试资源与当前代码契约不一致的问题
  - [x] SubTask 2.4: 为上述修复补充最小可回归验证

- [x] Task 3: 补齐 ETL 与标准化的 MVP 核心缺口
  - [x] SubTask 3.1: 基于现有 `cdm_patient` 能力设计多主题扩展的最小落地顺序
  - [x] SubTask 3.2: 至少补齐计划中缺失主题的最小闭环能力，覆盖 ODS、CDM、元数据与样例预览
  - [x] SubTask 3.3: 完善 ETL 任务日志、失败场景、增量游标与可验证路径
  - [x] SubTask 3.4: 增加针对新增主题与转换链路的验证

- [x] Task 4: 将报表设计器推进到 MVP 可验收状态
  - [x] SubTask 4.1: 补齐布局编辑、数据绑定和属性配置等核心交互
  - [x] SubTask 4.2: 保证预览、导出、模板存储之间的配置一致性
  - [x] SubTask 4.3: 打通报表任务、快照、站内通知的最小闭环
  - [x] SubTask 4.4: 为报表设计器关键流程补充验证

- [x] Task 5: 收敛开放 API 到计划要求的 MVP 水平
  - [x] SubTask 5.1: 将长期占位接口替换为真实可用的数据读取结果
  - [x] SubTask 5.2: 对齐令牌申请、scope、鉴权与访问日志行为
  - [x] SubTask 5.3: 将限流实现向计划要求收敛，并验证关键场景
  - [x] SubTask 5.4: 同步更新开放 API 文档与调用示例

- [x] Task 6: 补齐系统管理最小可运营能力
  - [x] SubTask 6.1: 识别当前系统管理中缺失的写操作闭环
  - [x] SubTask 6.2: 按 MVP 最小范围补齐用户、角色、权限或关键配置的维护能力
  - [x] SubTask 6.3: 确保写操作纳入权限控制与审计记录

- [x] Task 7: 强化测试、联调与交付验证
  - [x] SubTask 7.1: 补齐前端关键页面与交互的最小单测
  - [x] SubTask 7.2: 补齐后端关键服务、转换链路、开放 API、报表能力的最小测试
  - [x] SubTask 7.3: 建立面向 Docker 部署与端到端链路的回归验证步骤
  - [x] SubTask 7.4: 将验证结果沉淀为可复查的记录

- [x] Task 8: 收口文档、变更记录与进度台账
  - [x] SubTask 8.1: 更新部署文档、使用手册、接口说明和联调说明
  - [x] SubTask 8.2: 更新 `docs/项目进度说明.md`，反映最新阶段状态与剩余事项
  - [x] SubTask 8.3: 记录本轮迭代的变更范围、已完成项、验证结果和未完成项

# Task Dependencies

- Task 2 depends on Task 1
- Task 3 depends on Task 2
- Task 4 depends on Task 2
- Task 5 depends on Task 2
- Task 6 depends on Task 1
- Task 7 depends on Task 3, Task 4, Task 5, Task 6
- Task 8 depends on Task 2, Task 3, Task 4, Task 5, Task 6, Task 7
