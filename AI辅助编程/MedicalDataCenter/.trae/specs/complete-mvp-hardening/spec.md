# MedicalDataCenter MVP 收口增强 Spec

## Why
上一轮 `align-mvp-delivery` 已完成当前 MVP 主链路的偏差修复与最小闭环补齐，但根据 `medical-data-center-mvp-plan.md` 与 `docs/项目进度说明.md`，项目仍存在若干直接影响一期交付质量的剩余缺口。需要在不突破原定 MVP 范围的前提下，继续完成安全能力、系统管理闭环、测试体系与交付收口。

## What Changes
- 补齐剩余的 MVP 级安全能力，包括会话续期、退出登录、密码策略和账号锁定等最小闭环
- 将系统管理从“部分可运营”推进到“最小完整可运营”，补齐角色、系统参数等关键维护能力
- 补齐测试覆盖率统计、CI 驱动的验证步骤和交付验收记录
- 继续收敛 ETL、报表设计器、开放 API 的剩余文档与验证缺口，不新增超范围业务
- 强化项目变更、进度台账和交付物之间的一致性

## Impact
- Affected specs: 安全认证、系统管理、测试与交付、文档治理
- Affected code: `frontend/src/pages/system/**`、`frontend/src/api/modules/**`、`backend/src/main/java/com/medicaldatacenter/backend/auth/**`、`backend/src/main/java/com/medicaldatacenter/backend/system/**`、`backend/src/test/**`、`docs/**`、`tests/**`、CI/脚本配置

## ADDED Requirements
### Requirement: MVP 安全能力收口
系统 SHALL 在现有登录、JWT、RBAC 基础上，补齐符合 MVP 交付要求的最小安全管理闭环。

#### Scenario: 用户续期与退出
- **WHEN** 平台用户登录后访问受保护资源
- **THEN** 系统应提供受控的会话续期能力
- **THEN** 系统应提供退出登录能力，并使后续请求不能继续沿用失效会话

#### Scenario: 密码与账号保护
- **WHEN** 平台管理员创建或维护账号
- **THEN** 系统应执行最小密码策略校验
- **THEN** 系统应具备基础账号锁定或失败控制能力，防止无约束暴力尝试

### Requirement: 系统管理完整最小闭环
系统 SHALL 在现有用户写操作基础上，补齐角色、权限映射、系统参数等关键管理能力的最小闭环。

#### Scenario: 管理员维护角色与系统配置
- **WHEN** 管理员进入系统管理模块
- **THEN** 系统应支持角色维护、角色权限分配或关键系统参数维护中的最小必要集合
- **THEN** 所有写操作应纳入权限控制与审计日志

### Requirement: 测试覆盖与持续验证
系统 SHALL 建立最小可执行的覆盖率统计与持续验证路径，以支撑 MVP 一期交付验收。

#### Scenario: 执行阶段性交付验证
- **WHEN** 团队准备进行阶段性交付或回归验证
- **THEN** 系统应具备前端与后端的可执行测试入口
- **THEN** 应能输出覆盖率结果或覆盖范围记录
- **THEN** 应具备清晰的 CI/本地执行说明和失败排查入口

### Requirement: 交付物一致性收口
系统 SHALL 保证进度台账、接口说明、部署文档、联调资源、验证记录与当前代码实现保持同步。

#### Scenario: 完成本轮增强迭代
- **WHEN** 本轮功能、修复或测试增强完成
- **THEN** 必须同步更新 `docs/项目进度说明.md`
- **THEN** 必须同步更新测试说明、交付验证记录和相关接口文档
- **THEN** 必须保留可追溯的变更记录

## MODIFIED Requirements
### Requirement: 系统管理
系统 SHALL 从当前“用户最小写操作闭环”继续扩展到“角色与关键配置可维护”的 MVP 收口状态，但仍不演进为完整 IAM 平台。

#### Scenario: 角色与参数维护
- **WHEN** 管理员创建或修改角色、分配权限、调整关键系统参数
- **THEN** 系统应提供对应维护入口和接口
- **THEN** 变更后应可在系统管理页面与实际权限行为中被验证

### Requirement: 测试与交付
系统 SHALL 从“已具备最小测试与验证记录”推进到“具备覆盖率统计、可重复执行步骤和验收导向回归清单”的状态。

#### Scenario: 准备交付 MVP 一期
- **WHEN** 团队执行交付前回归
- **THEN** 必须能运行前端单测、后端测试、关键联调脚本和 Docker 部署验证
- **THEN** 必须能基于文档快速复现验证流程

### Requirement: 文档与进度台账
系统 SHALL 将剩余缺口、已完成状态、交付边界和未纳入本轮的事项明确写入进度台账，避免继续出现“已实现但文档仍显示缺失”或“文档规划过度超前”的偏差。

#### Scenario: 更新项目台账
- **WHEN** 本轮收口完成
- **THEN** 项目进度台账应反映当前真实状态
- **THEN** 应明确剩余非阻断增强项与后续建议顺序

## REMOVED Requirements
### Requirement: 超范围增强默认进入当前迭代
**Reason**: 当前阶段目标是完成 MVP 一期交付收口，而不是引入新的业务扩展。
**Migration**: 超出原始 MVP 范围的能力应记录为后续阶段候选项，不纳入本轮实现任务。
