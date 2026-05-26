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
  - 覆盖健康检查、登录、续期、退出、数据源测试、ETL、报表、开放 API。
- `postman/MedicalDataCenter.postman_collection.json`
  - 适合导入 Postman 进行团队协作联调。
  - 内置变量回填脚本，可自动保存管理端令牌与开放 API 令牌。
- `data/sample-dataset-mapping.json`
  - 提供 ODS 到 CDM 的样例字段映射，可作为抽取任务配置模板。

## 建议联调顺序

1. 启动 `docker/docker-compose.yml`
2. 调用健康检查接口
3. 验证管理端登录
4. 验证会话续期与退出登录
5. 测试数据源连接
6. 创建 ETL 任务
7. 创建报表模板
8. 获取 OAuth2 令牌
9. 调用开放 API

## 注意事项

- 当前仓库已提供可运行的 `frontend/` 与 `backend/` 工程，联调资源用于回归验证登录、ETL、报表、开放 API 与系统管理链路。
- 管理端认证链路已补齐 `login -> refresh -> logout`，续期后需使用新的 `accessToken / refreshToken`。
- `.http` 文件中的令牌变量需要手工回填，或由所用客户端工具自动提取。
- 本轮集成验证结论、Flyway 迁移顺序和 Docker 端到端回归步骤已沉淀到 `docs/交付验证记录.md`，可作为 Task7/Task8 的复查依据。

## 自动化测试与覆盖率

### 本地最小执行步骤

1. 后端测试与覆盖率：
   - 在 `backend/` 执行 `mvn clean verify`
   - 报告输出目录：`backend/target/site/jacoco/index.html`
2. 前端测试与覆盖率：
   - 在 `frontend/` 执行 `npm ci`
   - 执行 `npm run check:ci`
   - 报告输出目录：`frontend/coverage/index.html`
3. 端到端回归：
   - 执行 `pwsh ./scripts/docker-smoke.ps1`
   - 若需补充业务深度验证，再按 `docs/交付验证记录.md` 中的 Docker 主链路执行一次人工回归

### CI 最小执行步骤

- 已新增 GitHub Actions 工作流：`.github/workflows/ci.yml`
- 后端作业执行 `mvn -B clean verify`
- 前端作业执行 `npm ci` 与 `npm run check:ci`
- 容器回归作业 `compose-smoke` 会执行 `pwsh ./scripts/docker-smoke.ps1`
- `docker-smoke.ps1` 会拉起 `docker/docker-compose.yml`，并自动校验：
  - 后端健康检查
  - 前端首页
  - 管理端登录
  - `/api/auth/me`
  - `/oauth2/token`
  - `/open-api/v1/patients`
  - `/open-api/v1/medications`
- 覆盖率目录会作为构建产物上传，便于在失败或回归后复查
- 容器回归日志会上传 `.artifacts/compose-smoke/` 作为诊断制品

### 失败排查说明

- 后端 `mvn clean verify` 失败：
  - 优先查看 `backend/target/surefire-reports/` 与控制台首个 `Caused by`
  - 若启动阶段报 `Flyway` 重复版本，检查 `backend/src/main/resources/db/migration/` 是否存在重复 `V*__*.sql`
  - 若报数据库兼容问题，确认当前方言仍为 `TiDB / MySQL 8`，且未回退到 H2 专有语法
- 前端 `npm run check:ci` 失败：
  - `npm ci` 失败时先检查 `package-lock.json` 是否与 `package.json` 同步
  - `coverage` 失败时确认 `@vitest/coverage-v8` 已安装且 `frontend/coverage/` 可写
  - 若报 `window is not defined`，确认 `frontend/vite.config.ts` 的 Vitest 环境仍为 `jsdom`
  - `build` 失败时优先执行 `npm run type-check` 复现类型错误
- 覆盖率结果缺失：
  - 后端确认 `backend/target/site/jacoco/jacoco.xml` 与 `index.html` 是否生成
  - 前端确认 `frontend/coverage/coverage-final.json` 与 `index.html` 是否生成
- `compose-smoke` 失败：
  - 优先查看 CI 制品 `.artifacts/compose-smoke/compose-ps.log` 与 `compose-logs.log`
  - Windows 本地运行前先确认 Docker Desktop 已启动
  - 若卡在健康检查，优先确认当前宿主机端口 `18080/18081` 未被占用
  - 若卡在 OAuth2 或开放 API 校验，优先确认默认客户端 `demo-client / demo-secret` 仍存在，且 `V9` 迁移已成功执行
