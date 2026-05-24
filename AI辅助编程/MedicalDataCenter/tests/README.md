# 联调测试资源说明

`tests/` 目录用于承载医疗数据中心 MVP 一期的基础联调与样例数据资源。

## 目录结构

```text
tests/
├─ api/medical-data-center-smoke.http
├─ postman/MedicalDataCenter.postman_collection.json
└─ data/sample-dataset-mapping.json
```

## 资源说明

- `api/medical-data-center-smoke.http`
  - 适合使用 VS Code REST Client、JetBrains HTTP Client 等工具直接联调。
  - 覆盖健康检查、登录、数据源测试、ETL、报表、开放 API。
- `postman/MedicalDataCenter.postman_collection.json`
  - 适合导入 Postman 进行团队协作联调。
  - 内置变量回填脚本，可自动保存管理端令牌与开放 API 令牌。
- `data/sample-dataset-mapping.json`
  - 提供 ODS 到 CDM 的样例字段映射，可作为抽取任务配置模板。

## 建议联调顺序

1. 启动 `docker/docker-compose.yml`
2. 调用健康检查接口
3. 验证管理端登录
4. 测试数据源连接
5. 创建 ETL 任务
6. 创建报表模板
7. 获取 OAuth2 令牌
8. 调用开放 API

## 注意事项

- 当前仓库尚未提供可运行的 `frontend/` 与 `backend/` 工程，联调资源主要用于定义契约和后续接入验证。
- `.http` 文件中的令牌变量需要手工回填，或由所用客户端工具自动提取。
