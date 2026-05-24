# 医疗数据中心 API 接口说明

## 1. 文档说明

本文档基于 `medical-data-center-mvp-plan.md` 中的接口规划整理，作为 MVP 一期开放接口与管理接口的交付文档。实际开发阶段建议同步输出 `OpenAPI/Swagger` 文档并与本说明保持一致。

## 2. 通用约定

### 2.1 基础地址

| 场景 | 基础地址 |
| --- | --- |
| 管理端 API | `http://localhost:8080/api` |
| 网关转发后的管理端 API | `http://localhost/api` |
| 开放 API | `http://localhost/open-api/v1` |

### 2.2 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "9f4dd2f7-6b29-4dca-a721-6dd65b45f020",
  "timestamp": "2026-05-24T10:00:00Z"
}
```

### 2.3 常见状态码

| `code` | 含义 | 说明 |
| --- | --- | --- |
| `0` | 成功 | 请求处理成功 |
| `40001` | 参数错误 | 入参校验失败 |
| `40100` | 未认证 | 令牌缺失或失效 |
| `40300` | 无权限 | 角色、作用域或数据权限不足 |
| `40400` | 资源不存在 | 查询对象未找到 |
| `42900` | 请求过多 | 命中 Redis 限流规则 |
| `50000` | 系统异常 | 服务器内部错误 |

### 2.4 认证方式

- 管理端 API：登录后携带 `JWT`
- 开放 API：通过 `OAuth2 + JWT` 获取访问令牌

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

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9",
    "refreshToken": "refresh-token-value",
    "expiresIn": 7200,
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "username": "admin",
      "displayName": "系统管理员",
      "roles": ["SUPER_ADMIN"]
    }
  },
  "requestId": "request-id",
  "timestamp": "2026-05-24T10:00:00Z"
}
```

### 3.2 刷新令牌

- 方法：`POST`
- 路径：`/api/auth/refresh`

## 4. 数据源管理

### 4.1 查询数据源列表

- 方法：`GET`
- 路径：`/api/data-sources`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `pageNum` | integer | 否 | 页码 |
| `pageSize` | integer | 否 | 每页条数 |
| `keyword` | string | 否 | 名称关键字 |
| `type` | string | 否 | 数据源类型 |
| `status` | string | 否 | 连接状态 |

### 4.2 新增数据源

- 方法：`POST`
- 路径：`/api/data-sources`

请求体示例：

```json
{
  "name": "HIS-MySQL-01",
  "code": "his_mysql_01",
  "type": "MYSQL",
  "host": "10.10.0.12",
  "port": 3306,
  "databaseName": "his",
  "username": "his_readonly",
  "password": "<SECRET>",
  "properties": {
    "ssl": false,
    "connectTimeout": 5000
  }
}
```

### 4.3 测试连接

- 方法：`POST`
- 路径：`/api/data-sources/test`

说明：

- 不保存数据源，仅用于验证连通性与参数有效性。
- 应返回数据库版本、连通状态、耗时等信息。

### 4.4 切换数据源状态

- 方法：`PATCH`
- 路径：`/api/data-sources/{id}/status`

请求体示例：

```json
{
  "status": "DISABLED"
}
```

## 5. ETL 与标准化

### 5.1 查询抽取任务

- 方法：`GET`
- 路径：`/api/etl/jobs`

### 5.2 新增抽取任务

- 方法：`POST`
- 路径：`/api/etl/jobs`

请求体示例：

```json
{
  "name": "his_patient_increment_job",
  "sourceId": 1001,
  "sourceTable": "patient_info",
  "extractMode": "INCREMENTAL",
  "incrementField": "update_time",
  "odsTable": "ods_his_patient_info",
  "cdmEntity": "PATIENT",
  "cron": "0 */30 * * * ?",
  "fieldMappings": [
    {
      "sourceField": "patient_name",
      "targetField": "patient_name",
      "transformers": []
    },
    {
      "sourceField": "gender_code",
      "targetField": "gender",
      "transformers": ["DICT:GENDER"]
    }
  ]
}
```

### 5.3 执行抽取任务

- 方法：`POST`
- 路径：`/api/etl/jobs/{id}/run`

### 5.4 查询执行记录

- 方法：`GET`
- 路径：`/api/etl/runs/{runId}`

### 5.5 数据集分页查询

- 方法：`GET`
- 路径：`/api/datasets`

## 6. 报表设计与任务

### 6.1 新增报表模板

- 方法：`POST`
- 路径：`/api/reports/templates`

请求体示例：

```json
{
  "name": "门诊就诊趋势分析",
  "datasetId": 2001,
  "layoutSchema": {
    "version": "1.0.0",
    "components": [
      {
        "id": "chart_01",
        "type": "LINE",
        "title": "近30天门诊就诊趋势",
        "bindings": {
          "dimension": "visit_date",
          "metrics": ["visit_count"]
        }
      }
    ]
  }
}
```

### 6.2 报表预览

- 方法：`POST`
- 路径：`/api/reports/templates/{id}/preview`

### 6.3 导出 PDF

- 方法：`POST`
- 路径：`/api/reports/templates/{id}/export/pdf`

### 6.4 导出 Excel

- 方法：`POST`
- 路径：`/api/reports/templates/{id}/export/excel`

### 6.5 报表调度管理

- 方法：`GET/POST/PUT`
- 路径：`/api/reports/schedules`

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
  "clientName": "regional-health-platform",
  "clientType": "CONFIDENTIAL",
  "grantTypes": ["client_credentials"],
  "scopes": ["patient.read", "encounter.read", "lab.read"]
}
```

### 7.3 作用域列表

- 方法：`GET`
- 路径：`/api/open-api/scopes`

### 7.4 调用日志查询

- 方法：`GET`
- 路径：`/api/open-api/logs`

## 8. 第三方开放接口

### 8.1 获取访问令牌

- 方法：`POST`
- 路径：`/oauth2/token`
- 认证：`Basic client_id:client_secret`

请求示例：

```http
POST /oauth2/token HTTP/1.1
Authorization: Basic <BASE64(client_id:client_secret)>
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&scope=patient.read encounter.read
```

### 8.2 患者基础信息查询

- 方法：`GET`
- 路径：`/open-api/v1/patients`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `patientId` | string | 否 | 平台患者主键 |
| `idCardNo` | string | 否 | 身份证号，默认脱敏查询 |
| `name` | string | 否 | 患者姓名 |
| `pageNum` | integer | 否 | 页码 |
| `pageSize` | integer | 否 | 每页条数 |

响应数据示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "patientId": "P202605240001",
        "patientName": "张*",
        "gender": "M",
        "birthDate": "1985-03-21",
        "mobile": "138****0000"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 20
  },
  "requestId": "request-id",
  "timestamp": "2026-05-24T10:00:00Z"
}
```

### 8.3 就诊记录查询

- 方法：`GET`
- 路径：`/open-api/v1/encounters`

推荐查询参数：

- `patientId`
- `visitNo`
- `encounterType`
- `startDate`
- `endDate`

### 8.4 检验结果查询

- 方法：`GET`
- 路径：`/open-api/v1/labs`

推荐查询参数：

- `patientId`
- `encounterId`
- `itemCode`
- `startDate`
- `endDate`

### 8.5 报表结果查询

- 方法：`GET`
- 路径：`/open-api/v1/reports`

推荐查询参数：

- `reportCode`
- `snapshotDate`
- `pageNum`
- `pageSize`

## 9. 限流与审计

开放 API 应至少支持以下策略：

- 客户端级限流
- 接口级限流
- 突发流量保护
- 审计日志按 `clientId + uri + requestId` 检索

当命中限流规则时，返回示例：

```json
{
  "code": 42900,
  "message": "rate limit exceeded",
  "data": null,
  "requestId": "request-id",
  "timestamp": "2026-05-24T10:00:00Z"
}
```

## 10. 联调建议

推荐按以下顺序联调：

1. 登录接口与鉴权链路
2. 数据源新增与测试连接
3. ETL 任务创建与执行
4. 报表模板预览与导出
5. 开放客户端申请与令牌申请
6. 第三方查询接口与限流验证

基础联调资源见：

- `tests/api/medical-data-center-smoke.http`
- `tests/postman/MedicalDataCenter.postman_collection.json`
