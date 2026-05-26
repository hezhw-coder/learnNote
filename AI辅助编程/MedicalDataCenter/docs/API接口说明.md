# 医疗数据中心 API 接口说明

## 1. 文档说明

本文档描述当前项目已经实现并可用于联调的管理端 API、报表 API 和开放 API。由于项目同时存在开发版和生产版部署模式，基础地址需按场景选择。

## 2. 通用约定

### 2.1 基础地址

| 场景 | 基础地址 |
| --- | --- |
| 开发版管理端 API | `http://127.0.0.1:18080/api` |
| 开发版 Nginx 入口 | `http://127.0.0.1:18081` |
| 生产版统一入口 | `https://mdc.localtest.me:18443` |

说明：

- 生产版 `backend` 不直接暴露宿主机端口
- 生产版管理端 API、OAuth2 和开放 API 都应走 `https://mdc.localtest.me:18443`

### 2.2 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "9f4dd2f7-6b29-4dca-a721-6dd65b45f020",
  "timestamp": "2026-05-26T10:00:00Z"
}
```

### 2.3 常见状态码

| `code` | 含义 | 说明 |
| --- | --- | --- |
| `0` | 成功 | 请求处理成功 |
| `400` | 参数错误 | 入参校验失败、模板参数缺失、发布校验失败等 |
| `401` | 未认证 | Token 缺失、失效、退出后继续访问 |
| `403` | 无权限 | 权限码或作用域不足 |
| `404` | 资源不存在 | 模板、任务、客户端等不存在 |
| `409` | 版本冲突 | 报表模板发布或保存时 `baseVersion` 过期 |
| `423` | 账号锁定 | 连续登录失败触发锁定 |
| `429` | 请求过多 | 命中限流规则 |
| `500` | 系统异常 | 服务端内部错误 |

### 2.4 认证方式

- 管理端 API：平台账号登录后使用 `Authorization: Bearer <accessToken>`
- 开放 API：通过 `/oauth2/token` 获取访问令牌后使用 `Bearer`

请求头示例：

```http
Authorization: Bearer <access_token>
Content-Type: application/json
X-Request-Id: 7ad88973-a3b1-4afc-b6d9-9f9df53fbf82
```

## 3. 认证与用户

### 3.1 用户登录

- 方法：`POST`
- 路径：`/api/auth/login`

请求示例：

```json
{
  "username": "admin",
  "password": "<SECRET>"
}
```

关键返回字段：

- `accessToken`
- `refreshToken`
- `expiresIn`
- `refreshExpiresIn`
- `authorities`

### 3.2 会话续期

- 方法：`POST`
- 路径：`/api/auth/refresh`

### 3.3 退出登录

- 方法：`POST`
- 路径：`/api/auth/logout`

### 3.4 当前用户

- 方法：`GET`
- 路径：`/api/auth/me`

说明：

- 当前已实现服务端会话失效控制
- 退出登录后旧 `accessToken` 与 `refreshToken` 均不可继续使用

## 4. 数据源管理

### 4.1 查询数据源列表

- 方法：`GET`
- 路径：`/api/data-sources`

### 4.2 新增数据源

- 方法：`POST`
- 路径：`/api/data-sources`

请求体示例：

```json
{
  "name": "HIS-MySQL-01",
  "type": "MYSQL",
  "jdbcUrl": "jdbc:mysql://10.10.0.12:3306/his?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai",
  "databaseName": "his",
  "username": "his_readonly",
  "password": "<SECRET>"
}
```

### 4.3 测试连接

- 方法：`POST`
- 路径：`/api/data-sources/test`

### 4.4 切换数据源状态

- 方法：`PATCH`
- 路径：`/api/data-sources/{id}/status`

## 5. ETL 与数据集

### 5.1 查询 ETL 任务

- 方法：`GET`
- 路径：`/api/etl/jobs`

### 5.2 新增 ETL 任务

- 方法：`POST`
- 路径：`/api/etl/jobs`

当前建议使用语义化字段绑定：

```json
{
  "name": "his_patient_increment_job",
  "dataSourceId": 1001,
  "loadMode": "INCREMENTAL",
  "targetDatasetCode": "cdm_patient",
  "sourceTable": "patient_info",
  "incrementField": "update_time",
  "fieldBindings": {
    "patientCode": "patient_id",
    "patientName": "patient_name",
    "gender": "gender_code",
    "birthDate": "birth_date"
  }
}
```

说明：

- 后端会把 `fieldBindings` 持久化到 `etl_job.field_bindings_json`
- 历史 `idField/nameField/...` 仍兼容，但新调用建议统一改为 `fieldBindings`

### 5.3 执行 ETL 任务

- 方法：`POST`
- 路径：`/api/etl/jobs/{id}/run`

### 5.4 查询运行记录

- 方法：`GET`
- 路径：`/api/etl/runs/{runId}`

### 5.5 查询数据集

- 方法：`GET`
- 路径：`/api/datasets`

## 6. 报表设计与任务

### 6.1 查询模板列表

- 方法：`GET`
- 路径：`/api/reports/templates`

### 6.2 新增模板

- 方法：`POST`
- 路径：`/api/reports/templates`

### 6.3 更新模板

- 方法：`PUT`
- 路径：`/api/reports/templates/{id}`

### 6.4 发布模板

- 方法：`POST`
- 路径：`/api/reports/templates/{id}/publish`

说明：

- 模板当前支持 `draft/published` 双状态
- 发布时会执行最小结构校验
- 若 `baseVersion` 已过期，后端返回 `409`

### 6.5 模板结构关键字段

模板 `designJson` 当前已支持：

- `schemaVersion`
- `layout`
- `filters`
- `parameters`
- `widgets`
- `interactions`
- `versioning`

其中：

- `parameters` 用于模板级参数定义
- `versioning` 用于草稿版/发布版/生效版元数据

### 6.6 预览模板

- 方法：`GET`
- 路径：`/api/reports/templates/{id}/preview`
- 用途：无运行时参数时直接预览

- 方法：`POST`
- 路径：`/api/reports/templates/{id}/preview`
- 用途：带运行时参数或联动上下文预览

请求体示例：

```json
{
  "parameters": [
    {
      "parameterCode": "gender",
      "value": "女"
    }
  ],
  "interactions": [
    {
      "interactionId": "interaction-1",
      "sourceWidgetId": "chart-1",
      "targetWidgetId": "table-1",
      "sourceField": "gender",
      "targetField": "gender",
      "value": "女"
    }
  ]
}
```

当前返回结构重点包含：

- `templateName`
- `datasetCode`
- `layoutColumns`
- `widgetCount`
- `filterCount`
- `parameterSummary`
- `interactionSummary`
- `bindingSummary`
- `appliedFilters`
- `rows`

### 6.7 导出 PDF

- 方法：`GET`
- 路径：`/api/reports/templates/{id}/export/pdf`
- 用途：无运行时参数导出

- 方法：`POST`
- 路径：`/api/reports/templates/{id}/export/pdf`
- 用途：带运行时参数导出

### 6.8 导出 Excel

- 方法：`GET`
- 路径：`/api/reports/templates/{id}/export/excel`
- 用途：无运行时参数导出

- 方法：`POST`
- 路径：`/api/reports/templates/{id}/export/excel`
- 用途：带运行时参数导出

说明：

- 导出、预览、调度共用同一套模板归一化和过滤逻辑
- 快照会保留 `runtime_params_json`

### 6.9 调度管理

查询调度：

- 方法：`GET`
- 路径：`/api/reports/schedules`

新建调度：

- 方法：`POST`
- 路径：`/api/reports/schedules`

更新调度：

- 方法：`PUT`
- 路径：`/api/reports/schedules/{id}`

立即执行：

- 方法：`POST`
- 路径：`/api/reports/schedules/{id}/run`

请求体示例：

```json
{
  "templateId": 1,
  "cronExpression": "0 0 8 * * ?",
  "enabled": true,
  "channel": "IN_APP",
  "runtimeParams": {
    "parameters": [
      {
        "parameterCode": "gender",
        "value": "女"
      }
    ],
    "interactions": []
  }
}
```

当前返回结构重点包含：

- `latestRun`
- `nextRun`
- `owner`
- `templateVersion`

说明：

- 已启用任务会自动注册到 Quartz
- 应用重启后会自动恢复已启用调度
- 调度会记录 `template_version`
- 若调度绑定版本与模板当前生效版本不一致，执行会被阻断

### 6.10 快照列表

- 方法：`GET`
- 路径：`/api/reports/snapshots`

当前快照记录会保留：

- `scheduleId`
- `templateVersion`
- `runtimeParamsJson`
- 统一渲染快照内容

## 7. 开放 API 管理

### 7.1 客户端列表

- 方法：`GET`
- 路径：`/api/open-api/clients`

### 7.2 新增客户端

- 方法：`POST`
- 路径：`/api/open-api/clients`

请求体示例：

```json
{
  "name": "regional-health-platform",
  "clientId": "regional-health-platform",
  "clientSecret": "<SECRET>",
  "scopes": "patients.read,encounters.read,labs.read,medications.read,reports.read"
}
```

### 7.3 作用域列表

- 方法：`GET`
- 路径：`/api/open-api/scopes`

### 7.4 调用日志查询

- 方法：`GET`
- 路径：`/api/open-api/logs`

## 8. 系统管理

当前已开放的核心接口包括：

- `GET /api/system/users`
- `POST /api/system/users`
- `PUT /api/system/users/{id}/status`
- `PUT /api/system/users/{id}/roles`
- `GET /api/system/roles`
- `POST /api/system/roles`
- `PUT /api/system/roles/{id}`
- `PUT /api/system/roles/{id}/permissions`
- `GET /api/system/permissions`
- `GET /api/system/parameters`
- `PUT /api/system/parameters/{key}`
- `GET /api/system/overview`

权限要求：

- 用户写操作：`SYSTEM_USER_MANAGE`
- 角色写操作：`SYSTEM_ROLE_MANAGE`
- 参数写操作：`SYSTEM_PARAM_MANAGE`

## 9. 第三方开放接口

### 9.1 获取访问令牌

- 方法：`POST`
- 路径：`/oauth2/token`

支持两种调用方式：

1. `Authorization: Basic <base64(client_id:client_secret)> + application/x-www-form-urlencoded`
2. `application/json` 直接传 `clientId/clientSecret/scope`

推荐统一通过 Nginx 入口调用：

- 开发版：`http://127.0.0.1:18081/oauth2/token`
- 生产版：`https://mdc.localtest.me:18443/oauth2/token`

### 9.2 患者查询

- 方法：`GET`
- 路径：`/open-api/v1/patients`
- 作用域：`patients.read`

推荐参数：

- `patientCode`
- `gender`
- `nameKeyword`
- `startDate`
- `endDate`

### 9.3 就诊查询

- 方法：`GET`
- 路径：`/open-api/v1/encounters`
- 作用域：`encounters.read`

推荐参数：

- `patientCode`
- `encounterType`
- `departmentName`
- `doctorName`
- `startDate`
- `endDate`

### 9.4 检验查询

- 方法：`GET`
- 路径：`/open-api/v1/labs`
- 作用域：`labs.read`

推荐参数：

- `patientCode`
- `encounterCode`
- `itemCode`
- `itemName`
- `abnormalOnly`
- `startDate`
- `endDate`

### 9.5 用药查询

- 方法：`GET`
- 路径：`/open-api/v1/medications`
- 作用域：`medications.read`

推荐参数：

- `patientCode`
- `drugCode`
- `drugName`
- `startDate`
- `endDate`

### 9.6 报表结果查询

- 方法：`GET`
- 路径：`/open-api/v1/reports`
- 作用域：`reports.read`

推荐参数：

- `reportCode`
- `snapshotDate`
- `pageNum`
- `pageSize`

## 10. 限流与审计

当前开放 API 已具备：

- 客户端级限流
- Redis 优先、进程内回退
- 访问日志
- `requestId` 透传
- 成功日志统一记录为 `ok rows=<n>`

说明：

- 平台管理端 token 不可直接访问 `/open-api/**`
- scope 不足返回 `403`
- 命中限流返回 `429`
- 成功请求会写入 `api_access_log`

## 11. 联调建议

推荐顺序：

1. 登录、续期、退出登录
2. 数据源新增与测试连接
3. ETL 任务创建与执行
4. 报表模板预览、发布、导出、调度
5. 客户端申请与 token 申请
6. 患者/就诊/检验/用药开放接口验证

联调资源：

- `tests/api/medical-data-center-smoke.http`
- `tests/postman/MedicalDataCenter.postman_collection.json`
