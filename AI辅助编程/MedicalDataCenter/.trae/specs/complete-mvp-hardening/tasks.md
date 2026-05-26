# Tasks

- [x] Task 1: 梳理本轮 MVP 收口范围并冻结剩余缺口
  - [x] SubTask 1.1: 对照 `medical-data-center-mvp-plan.md` 与 `docs/项目进度说明.md` 提取仍未完成但属于原始 MVP 的事项
  - [x] SubTask 1.2: 将剩余事项归类为“安全能力 / 系统管理 / 测试与 CI / 文档收口”
  - [x] SubTask 1.3: 明确本轮不纳入实现的超范围增强项

- [x] Task 2: 补齐 MVP 最小安全闭环
  - [x] SubTask 2.1: 设计并实现会话续期与退出登录能力
  - [x] SubTask 2.2: 增加最小密码策略校验与账号保护机制
  - [x] SubTask 2.3: 为安全能力补充接口说明、页面交互与回归验证

- [x] Task 3: 补齐系统管理剩余关键写操作
  - [x] SubTask 3.1: 识别角色、权限映射、系统参数等仍缺失的维护能力
  - [x] SubTask 3.2: 按 MVP 最小范围补齐后端接口与前端页面闭环
  - [x] SubTask 3.3: 确保关键写操作纳入权限控制与审计日志

- [x] Task 4: 建立覆盖率与持续验证基础
  - [x] SubTask 4.1: 为前端与后端补齐覆盖率统计入口或可替代的覆盖范围输出
  - [x] SubTask 4.2: 明确本地与 CI 的最小执行步骤
  - [x] SubTask 4.3: 补足关键模块测试缺口并沉淀失败排查说明

- [x] Task 5: 收口交付文档与进度台账
  - [x] SubTask 5.1: 更新 `docs/项目进度说明.md`，反映本轮收口后的真实状态
  - [x] SubTask 5.2: 更新部署文档、接口文档、测试说明与交付验证记录
  - [x] SubTask 5.3: 明确 MVP 一期已完成边界、遗留增强项与后续建议

- [x] Task 6: 复核 Flyway 迁移顺序文档并完成最终收尾验证
  - [x] SubTask 6.1: 对照 `backend/src/main/resources/db/migration/` 实际脚本顺序修正部署文档中的过时描述
  - [x] SubTask 6.2: 同步校正文档中其他 Flyway 版本表述，避免与当前线性迁移历史不一致
  - [x] SubTask 6.3: 复查 `checklist.md` 与交付验证记录，确认本轮检查项全部保持通过

# Task Dependencies

- Task 2 depends on Task 1
- Task 3 depends on Task 1
- Task 4 depends on Task 2, Task 3
- Task 5 depends on Task 2, Task 3, Task 4
- Task 6 depends on Task 5
