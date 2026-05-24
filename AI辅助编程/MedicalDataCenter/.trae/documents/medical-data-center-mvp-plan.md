# 医疗数据中心项目实施计划（MVP一期）

## 一、摘要

本项目按“前后端分离 + Docker 部署 + MVP 一期”实施，建设一个可独立部署的医疗数据中心平台。前端采用 `Vite + Vue + TypeScript + npm`，后端采用 `Spring Boot + Maven`，围绕数据库连接管理、多源医疗数据抽取集成、在线报表设计器、开放 API 服务四大核心模块交付一套可部署、可测试、可演示、可扩展的系统。

本期技术与产品决策如下：

- 部署方式：Docker 容器化部署。
- 平台主数据库：`TiDB`（作为国产数据库主库）。
- 用户与权限：平台内置账号体系，采用 `RBAC`。
- 报表设计器：低代码拖拽版。
- 标准化策略：双层模型，包含原始层（ODS）与标准层（CDM）。
- 开放 API 鉴权：`OAuth2 + JWT`。
- API 限流：`Redis` 令牌桶。
- 报表推送：站内消息；邮件仅预留接口能力，本期不实际发送。
- 合规基线：按行业基础合规并兼顾等保要求设计。

## 二、当前状态分析

### 2.1 仓库现状

经实际勘察，当前仓库 `e:\trae_learning\MedicalDataCenter` 为空目录，尚未存在以下内容：

- 前端工程
- 后端工程
- 数据库脚本
- Docker 编排文件
- 测试工程
- 项目文档

这意味着本次计划按照全新项目初始化方式设计目录结构、模块边界和交付顺序，不需要兼容既有代码实现。

### 2.2 约束与已确认需求

- 前后端必须独立部署、独立构建、接口规范统一。
- 前端必须使用 `Vite + Vue + TypeScript`，并采用 `npm` 包管理。
- 后端必须使用 `Spring Boot`，并采用 `Maven` 包管理。
- 平台须覆盖数据库连接配置、抽取集成、报表设计、开放 API 四大业务域。
- 需要交付单元测试、联调测试、安装包、部署文档与使用手册。
- 需要满足医疗行业基础数据安全合规要求，并预留等保增强点。

### 2.3 规划性假设

以下假设用于让方案达到“可直接实施”：

- “以上最好都适配”解释为：首期强制验收覆盖 `MySQL`、`PostgreSQL`、`SQL Server`、`Oracle`、`MongoDB`、`TiDB` 六类数据源。
- 对其他关系型数据库采用 `JDBC/SPI` 适配扩展机制预留，不纳入 MVP 强制验收。
- 中央平台唯一主业务库与标准化仓库主存储采用 `TiDB`。
- 对象文件存储不是本期必需能力，报表导出文件先存本地挂载卷。
- 邮件推送本期仅保留配置模型、任务模型和适配器接口，不要求真实发送成功。

## 三、目标架构

### 3.1 仓库结构

计划采用单仓库、双工程、独立部署结构：

```text
e:\trae_learning\MedicalDataCenter
├─ frontend/                         # Vue + Vite 前端
├─ backend/                          # Spring Boot 后端
├─ docker/                           # Docker 镜像与初始化资源
├─ scripts/                          # 启动、打包、初始化辅助脚本
├─ docs/                             # 面向交付的部署文档、使用手册
├─ tests/                            # 联调测试脚本与样例数据
└─ .trae/documents/                  # 计划与产品/技术文档
```

### 3.2 逻辑分层

- 前端层：管理控制台、报表设计器、连接配置页面、抽取任务页面、开放 API 管理页面。
- 应用层：统一 REST API、认证鉴权、任务编排、报表渲染、开放接口服务。
- 集成层：JDBC 数据源适配器、MongoDB 适配器、标准化转换器、调度器、消息通知适配器。
- 数据层：TiDB 主库/仓库、Redis 限流与缓存、导出文件卷、审计日志表。

### 3.3 核心数据流

1. 管理员在前端录入数据源连接参数并发起连通性测试。
2. 后端加密保存连接配置，维护状态并生成数据源元数据。
3. 数据工程师创建抽取任务，选择全量或增量策略。
4. 调度器调用对应数据源适配器，抽取原始数据进入 `ODS`。
5. 清洗转换模块将 ODS 数据映射到标准层 `CDM`。
6. 报表设计器从标准化数据集读取字段与样例数据完成设计。
7. 报表预览与导出由后端渲染服务生成 PDF/Excel 文件。
8. 第三方通过 OAuth2/JWT 获取令牌并调用开放 API。

## 四、拟建设计

### 4.1 前端工程 `frontend/`

#### 4.1.1 技术选型

- `Vite`
- `Vue 3`
- `TypeScript`
- `Vue Router`
- `Pinia`
- `Element Plus`
- `VueUse`
- `ECharts`
- `@vue-flow/core` 或 `GridStack` 类拖拽布局能力
- `Vitest + Vue Test Utils`

#### 4.1.2 页面与模块

1. 登录页 `/login`
   - 账号密码登录
   - JWT 会话初始化
   - 首次密码修改与过期提示预留

2. 工作台 `/dashboard`
   - 数据源总览
   - 抽取任务统计
   - 报表运行统计
   - API 调用概览

3. 数据源管理 `/data-sources`
   - 数据源列表、筛选、分页
   - 新增/编辑连接配置
   - 测试连接
   - 启停状态切换
   - 删除与软删除确认

4. 抽取集成 `/etl`
   - 任务列表
   - 新建抽取任务
   - 全量/增量策略配置
   - 字段映射、清洗规则配置
   - 执行日志查看

5. 数据集管理 `/datasets`
   - 标准化数据集目录
   - 字段血缘概览
   - 样本数据预览

6. 报表设计器 `/reports/designer/:id`
   - 左侧组件面板
   - 中央画布拖拽布局
   - 右侧属性面板
   - 数据绑定面板
   - 预览与导出

7. 报表任务 `/reports/jobs`
   - 定时任务配置
   - 生成记录
   - 站内消息查看
   - 邮件推送配置占位

8. 开放 API 管理 `/open-api`
   - 客户端应用管理
   - 凭证与作用域管理
   - 接口目录
   - 调用日志
   - 限流策略查看

9. 系统管理 `/system`
   - 用户、角色、权限
   - 字典项
   - 审计日志
   - 系统参数

#### 4.1.3 目录规划

```text
frontend/
├─ src/
│  ├─ api/
│  ├─ assets/
│  ├─ components/
│  │  ├─ common/
│  │  ├─ charts/
│  │  ├─ datasource/
│  │  ├─ etl/
│  │  └─ report-designer/
│  ├─ composables/
│  ├─ layouts/
│  ├─ pages/
│  ├─ router/
│  ├─ stores/
│  ├─ types/
│  └─ utils/
├─ public/
└─ package.json
```

#### 4.1.4 关键实现点

- 统一封装请求客户端、鉴权拦截器、错误处理、权限指令。
- 连接配置表单按数据库类型动态切换参数项。
- 报表设计器采用“组件树 + 布局树 + 数据绑定配置”的 JSON Schema 存储。
- 图表组件统一采用可序列化配置模型，避免页面逻辑与图表配置耦合。
- 页面级权限通过路由元数据与按钮级指令共同控制。

### 4.2 后端工程 `backend/`

#### 4.2.1 技术选型

- `Spring Boot 3.x`
- `Spring Web`
- `Spring Security`
- `Spring Authorization Server` 或轻量 OAuth2 授权实现
- `Spring Validation`
- `MyBatis-Plus` 或 `Spring Data JDBC/JPA`（建议选 `MyBatis-Plus`）
- `Quartz` 或 `Spring Scheduler`（建议选 `Quartz`）
- `Redis`
- `TiDB` JDBC 驱动
- 多数据源 JDBC 驱动
- `MongoDB Java Driver`
- `JUnit 5 + Mockito + Spring Boot Test`
- `Testcontainers`（用于集成测试）

#### 4.2.2 模块拆分

```text
backend/
├─ src/main/java/.../
│  ├─ common/               # 通用返回、异常、工具、枚举
│  ├─ config/               # 安全、Redis、Swagger、Jackson、调度配置
│  ├─ auth/                 # 登录、JWT、OAuth2、客户端凭证
│  ├─ system/               # 用户、角色、权限、审计日志
│  ├─ datasource/           # 连接配置、测试、状态管理、适配器注册
│  ├─ etl/                  # 任务配置、执行器、清洗转换、增量游标
│  ├─ warehouse/            # ODS/CDM 模型、数据集管理、元数据
│  ├─ report/               # 报表模板、设计 JSON、预览导出、任务调度
│  ├─ openapi/              # 开放接口、限流、访问日志、授权范围
│  ├─ notification/         # 站内消息、邮件接口预留
│  └─ infrastructure/       # SPI、文件存储、加解密、驱动工厂
├─ src/main/resources/
│  ├─ mapper/
│  ├─ db/migration/
│  └─ application-*.yml
└─ pom.xml
```

#### 4.2.3 核心领域模型

- `sys_user`：平台用户
- `sys_role`：角色
- `sys_permission`：权限点
- `sys_audit_log`：审计日志
- `ds_source`：数据源定义
- `ds_source_secret`：数据源敏感配置密文
- `etl_job`：抽取任务
- `etl_job_run`：抽取执行记录
- `etl_increment_checkpoint`：增量抽取游标
- `ods_*`：原始层表
- `cdm_*`：标准层表
- `meta_dataset`：数据集元数据
- `meta_dataset_field`：字段元数据
- `report_template`：报表模板
- `report_snapshot`：报表快照
- `report_schedule`：报表调度配置
- `notify_message`：站内消息
- `api_client`：开放 API 客户端
- `api_scope`：开放 API 权限域
- `api_access_log`：开放接口访问日志
- `api_rate_limit_rule`：限流规则

#### 4.2.4 数据源适配策略

- 关系型数据库统一抽象 `RelationalSourceAdapter` 接口。
- `MySQL`、`PostgreSQL`、`SQL Server`、`Oracle`、`TiDB` 通过 JDBC 方言实现。
- `MongoDB` 使用专用 `DocumentSourceAdapter`。
- 统一支持以下能力：
  - 连通性测试
  - 元数据探测
  - 分页抽取
  - 全量抽取
  - 基于时间戳/主键/业务字段的增量抽取
- 数据源能力矩阵在系统中维护，避免前端暴露不支持的策略。

#### 4.2.5 ODS/CDM 双层建模

- `ODS`：保留原始表结构或最小清洗后的镜像数据，按数据源、表、批次管理。
- `CDM`：构建医疗主题标准表，首期至少覆盖：
  - 患者
  - 就诊
  - 科室
  - 医生
  - 医嘱
  - 检验
  - 检查
  - 药品
- 通过字段映射规则、字典转换规则、清洗规则将 ODS 转换为 CDM。

### 4.3 开放 API 方案

#### 4.3.1 接口范围

- 患者基础信息查询
- 就诊记录查询
- 检验结果查询
- 报表结果查询
- 数据集分页查询
- API 客户端管理与令牌申请

#### 4.3.2 安全设计

- 基于 OAuth2 获取访问令牌。
- JWT 内包含客户端、作用域、过期时间、签发方。
- 敏感字段默认脱敏输出，可按作用域控制是否返回完整字段。
- 接口访问全量记录审计日志。
- 使用 Redis 令牌桶进行客户端级、接口级限流。
- 预留 IP 白名单、签名校验和双向 TLS 扩展点。

### 4.4 报表设计器方案

#### 4.4.1 设计对象模型

- 页面布局：行、列、栅格、组件占位
- 组件类型：折线图、柱状图、饼图、数据表格、指标卡
- 数据绑定：数据集、维度、指标、过滤器
- 样式属性：标题、颜色、字号、图例、坐标轴、边框、背景

#### 4.4.2 存储方式

- 报表模板使用 JSON 保存设计结构。
- 预览时由后端解析模板并查询对应数据集。
- 导出 PDF/Excel 时由后端执行同一份模板渲染逻辑，保证预览与导出一致。

#### 4.4.3 调度与推送

- 报表调度使用 `Quartz`。
- 执行结果生成快照和导出文件。
- 首期支持站内消息通知。
- 邮件推送定义接口与配置模型，但实现保持空适配器。

### 4.5 合规与安全基线

- 用户登录、客户端访问统一鉴权。
- 数据源密码、令牌密钥等敏感信息加密存储。
- 全链路 HTTPS 反向代理接入说明纳入部署文档。
- 审计日志覆盖登录、连接变更、抽取执行、报表发布、开放 API 调用。
- 敏感字段脱敏展示与返回。
- 删除操作采用逻辑删除或软删除策略。
- 预留数据备份恢复脚本与恢复流程说明。
- 权限模型覆盖菜单、页面动作、数据操作。

## 五、具体文件规划

### 5.1 前端计划新增文件

- `frontend/package.json`：前端依赖与脚本
- `frontend/vite.config.ts`：构建配置
- `frontend/src/main.ts`：应用入口
- `frontend/src/router/index.ts`：路由定义
- `frontend/src/stores/auth.ts`：登录态管理
- `frontend/src/stores/permission.ts`：权限管理
- `frontend/src/api/http.ts`：统一请求封装
- `frontend/src/api/modules/*.ts`：各业务 API
- `frontend/src/pages/*`：各业务页面
- `frontend/src/components/report-designer/*`：报表设计器组件
- `frontend/src/components/datasource/*`：数据源表单与列表组件
- `frontend/src/components/etl/*`：抽取任务组件
- `frontend/src/types/*`：前端领域类型
- `frontend/src/utils/schema.ts`：报表配置序列化工具

### 5.2 后端计划新增文件

- `backend/pom.xml`：后端依赖与构建配置
- `backend/src/main/java/.../Application.java`：启动类
- `backend/src/main/resources/application.yml`：主配置
- `backend/src/main/resources/application-dev.yml`：开发环境配置
- `backend/src/main/resources/application-docker.yml`：容器环境配置
- `backend/src/main/resources/db/migration/V*.sql`：数据库初始化脚本
- `backend/src/main/java/.../config/*`：基础配置
- `backend/src/main/java/.../controller/*`：REST 控制器
- `backend/src/main/java/.../service/*`：服务层
- `backend/src/main/java/.../repository/*`：数据访问层
- `backend/src/main/java/.../domain/*`：领域实体
- `backend/src/main/java/.../dto/*`：请求响应对象
- `backend/src/main/java/.../adapter/*`：数据源适配器实现
- `backend/src/test/java/.../*`：单元测试与集成测试

### 5.3 部署与文档文件

- `docker/docker-compose.yml`：统一编排
- `docker/frontend/Dockerfile`：前端镜像
- `docker/backend/Dockerfile`：后端镜像
- `docker/nginx/default.conf`：反向代理配置
- `scripts/build.ps1`：Windows 构建脚本
- `scripts/build.sh`：Linux 构建脚本
- `docs/部署文档.md`：部署说明
- `docs/使用手册.md`：操作手册
- `docs/API接口说明.md`：开放接口文档
- `tests/api/*.http` 或 `tests/postman/*`：联调测试资源

## 六、接口与契约规划

### 6.1 统一响应格式

后端 REST 接口统一采用：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "string",
  "timestamp": "2026-05-24T10:00:00Z"
}
```

### 6.2 关键接口分组

1. 认证与用户
   - `/api/auth/login`
   - `/api/auth/refresh`
   - `/api/system/users`
   - `/api/system/roles`

2. 数据源管理
   - `/api/data-sources`
   - `/api/data-sources/{id}`
   - `/api/data-sources/test`
   - `/api/data-sources/{id}/status`

3. 抽取与标准化
   - `/api/etl/jobs`
   - `/api/etl/jobs/{id}/run`
   - `/api/etl/runs/{runId}`
   - `/api/datasets`

4. 报表设计与任务
   - `/api/reports/templates`
   - `/api/reports/templates/{id}/preview`
   - `/api/reports/templates/{id}/export/pdf`
   - `/api/reports/templates/{id}/export/excel`
   - `/api/reports/schedules`

5. 开放 API 管理
   - `/api/open-api/clients`
   - `/api/open-api/scopes`
   - `/api/open-api/logs`

6. 第三方开放接口
   - `/open-api/v1/patients`
   - `/open-api/v1/encounters`
   - `/open-api/v1/labs`
   - `/open-api/v1/reports`

### 6.3 前后端契约管理

- 采用 `OpenAPI/Swagger` 自动生成接口文档。
- DTO 与字段字典统一命名，避免多套协议口径。
- 前端 `src/types` 与后端 DTO 保持一一映射。
- 抽取任务配置、报表模板 JSON、限流规则均采用显式 schema。

## 七、实施步骤

### 7.1 第 1 阶段：工程初始化

- 初始化 `frontend` Vite Vue TypeScript 工程。
- 初始化 `backend` Spring Boot Maven 工程。
- 建立基础目录结构、统一代码规范、环境变量模板。
- 建立 Docker Compose 编排骨架。

### 7.2 第 2 阶段：基础设施与安全底座

- 完成登录认证、RBAC、JWT、审计日志基础能力。
- 完成 TiDB、Redis、文件导出目录配置。
- 完成统一异常、统一响应、请求日志、配置加密能力。

### 7.3 第 3 阶段：数据源管理模块

- 实现数据源新增、编辑、删除、状态切换。
- 实现多类型连接参数表单与连通性测试。
- 实现敏感配置加密存储。
- 完成数据源列表与详情 API。

### 7.4 第 4 阶段：ETL 与标准化模块

- 实现多适配器抽取框架。
- 实现全量/增量任务配置与执行。
- 实现 ODS 入仓与 CDM 转换。
- 实现任务日志、失败重试、游标保存。

### 7.5 第 5 阶段：报表设计器模块

- 搭建拖拽画布、组件库、属性面板、数据绑定面板。
- 实现模板保存、预览、导出。
- 实现定时任务与站内消息通知。
- 预留邮件推送接口。

### 7.6 第 6 阶段：开放 API 模块

- 实现开放客户端管理。
- 实现 OAuth2/JWT 授权流程。
- 实现第三方查询接口。
- 实现 Redis 限流和访问日志。

### 7.7 第 7 阶段：测试、联调与交付

- 完成前端单测、后端单测、集成测试。
- 完成前后端联调和容器化部署验证。
- 输出部署文档、使用手册、接口文档与打包产物。

## 八、验证方案

### 8.1 单元测试

- 前端：对核心页面、表单校验、权限控制、报表设计器 JSON 序列化编写 `Vitest` 测试。
- 后端：对服务层、适配器、增量抽取算法、限流组件、权限校验编写 `JUnit 5` 测试。

### 8.2 集成测试

- 使用 `Testcontainers` 组合 `TiDB`、`Redis`、必要的示例源库。
- 验证数据源连接测试、抽取任务执行、报表导出、开放 API 鉴权与限流。

### 8.3 联调测试

- 验证前端连接配置到后端持久化全链路。
- 验证抽取任务从源库到 ODS/CDM 的端到端流程。
- 验证报表预览与导出结果一致性。
- 验证第三方 OAuth2/JWT 获取令牌并成功访问开放 API。

### 8.4 验收标准

- 六类数据源均可完成连接配置与连通性测试。
- 至少一条全量任务和一条增量任务可成功执行并写入 ODS/CDM。
- 报表设计器可完成拖拽、绑定、预览、导出 PDF/Excel。
- 开放 API 可完成鉴权、授权访问、限流与日志记录。
- Docker Compose 可在标准环境一键启动。
- 部署文档、使用手册、接口文档齐备。

## 九、风险与应对

- 多数据源驱动兼容风险：通过适配器 SPI 和能力矩阵隔离差异。
- Oracle/SQL Server 本地测试环境复杂：优先用容器或测试替身验证核心逻辑。
- 报表拖拽复杂度高：先交付低代码网格版，不实现自由像素级布局。
- 医疗标准口径差异大：以平台标准模型为主，字段映射规则可配置。
- 国产数据库兼容性问题：平台主库选 TiDB，关系型源库通过方言隔离。

## 十、执行决策

本计划对应的实现决策如下，不再在开发阶段重复讨论：

- 仓库采用 `frontend + backend + docker + docs + tests` 结构。
- 前端状态管理采用 `Pinia`，组件库采用 `Element Plus`。
- 后端数据访问采用 `MyBatis-Plus`。
- 调度采用 `Quartz`。
- 平台主库采用 `TiDB`，缓存与限流采用 `Redis`。
- 抽取框架采用“关系型统一适配 + MongoDB 专用适配”双路线。
- 报表设计模板采用 JSON Schema 存储。
- 邮件推送接口仅预留，不做真实发信。

