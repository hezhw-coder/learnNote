# Debug Session: startup-no-output
- **Status**: [OPEN]
- **Issue**: 项目启动失败，用户反馈“跑不起来，没有输出”
- **Debug Server**: Pending
- **Log File**: .dbg/trae-debug-log-startup-no-output.ndjson

## Reproduction Steps
1. 在 `e:\trae_learning\MedicalDataCenter` 通过 `Docker Compose` 启动整个项目。
2. 终端表现为“一直卡住”，没有看到足够的有效输出。
3. 当前用户更方便提供终端截图/输出，尚未提供准确命令原文。

## Hypotheses & Verification
| ID | Hypothesis | Likelihood | Effort | Evidence |
|----|------------|------------|--------|----------|
| A | 实际并未进入项目启动阶段，而是命令执行器/终端包装层直接失败，导致看起来“没有输出” | High | Low | Rejected |
| B | 本机缺少 `Docker`、`Node/npm`、`Java/Maven` 中的关键运行时，启动命令立即退出 | High | Low | Confirmed |
| C | 启动命令或工作目录不对，例如在错误目录执行、脚本未被调用、调用了错误入口 | High | Low | Confirmed |
| D | Docker 或后端/前端构建过程被依赖下载、镜像构建、权限或网络问题卡住，但用户未看到日志 | Med | Med | Confirmed |
| E | 应用实际启动后马上崩溃，但日志未被捕获到当前终端或输出被重定向 | Med | Med | Rejected |

## Log Evidence
- 已知启动方式：`Docker Compose`
- 已知现象：终端卡住
- `docker version` 返回 `open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`
- 说明 Docker Client 存在，但 Docker Desktop Linux Engine / daemon 未启动或不可用
- 当前终端目录为 `E:\trae_learning\MedicalDataCenter\frontend`
- 执行 `docker compose -f .\docker\docker-compose.yml ...` 实际解析为 `frontend\docker\docker-compose.yml`
- 错误为 `The system cannot find the path specified`，说明 compose 文件路径使用错误
- 用户已确认当前 Docker Desktop 处于“未启动/未打开”状态
- 用户重新执行 `docker compose -f .\docker\docker-compose.yml up --build` 后，`redis`、`tidb` 已成功拉取
- 构建阶段失败于基础镜像元数据拉取：`node:20-alpine`、`nginx:1.27-alpine`、`eclipse-temurin:17-jre-jammy`、`maven:3.9.8-eclipse-temurin-17`
- 关键报错：`failed to resolve source metadata ... Head "https://hub-mirror.c.163.com/...": EOF`
- 说明 Docker 当前配置了 `hub-mirror.c.163.com` 镜像源，且该镜像源访问异常，导致基础镜像无法拉取
- 用户调整镜像源后重新执行构建，现已进入项目镜像构建阶段
- 新的阻塞点为后端 Maven 构建：`'dependencies.dependency.version' for org.flywaydb:flyway-database-h2:jar is missing`
- 前端 `npm install` 被 `compose` 连带取消，不是前端自身根因
- 用户进一步反馈当前现象为卡在 `RUN mvn -q -DskipTests dependency:go-offline`
- 已对 `docker/backend/Dockerfile` 做最小化观测增强：去掉 `-q`，改为 `mvn -B -e ...`，以便暴露真实卡点
- Maven 详细日志已确认根因：`org.flywaydb:flyway-database-h2:jar:10.20.1` 在 Maven Central 不存在
- 已据此做最小修复：从 `backend/pom.xml` 移除无效的 `flyway-database-h2` 依赖，仅保留 `flyway-core`
- 后续编译日志又暴露新的确定性问题：`SecuritySupport.java` 中 `List<SimpleGrantedAuthority>` 不能赋值给 `List<GrantedAuthority>`
- 已做最小修复：在权限映射处显式转为 `GrantedAuthority`
- 继续静态排查启动阶段后，确认 `V1__init_schema.sql` 使用了多处 H2 专属/偏 H2 语法，不兼容 `docker` 环境下的 `TiDB/MySQL`
- 已将 migration 改写为更兼容 `TiDB/MySQL` 的语法：`auto_increment`、`longtext`、`insert ignore`
- 用户提供新的运行时堆栈：`JwtAuthenticationFilter -> JwtService` 构造链失败
- 已确认根因是 docker 默认 `MDC_JWT_SECRET` 太短，不满足 HS256 生成密钥的最小长度要求
- 已做最小修复：更新 docker 默认 JWT 密钥，并在 `JwtService` 中增加显式长度校验与清晰错误信息
- 最新后端日志显示数据源已连通到 TiDB，但启动失败于数据库不存在：`Unknown database 'medical_data_center'`
- 已做最小修复：在 docker 环境 JDBC URL 上增加 `createDatabaseIfNotExist=true`
- `createDatabaseIfNotExist=true` 在当前组合下未生效，后端仍报同一数据库不存在错误
- 已切换为更可靠的 compose 侧初始化方案：新增 `tidb-init` 一次性建库服务，并让 `backend` 在建库成功后再启动
- 最新后端日志显示 `Flyway` 初始化失败：`Unsupported Database: MySQL 8.0`
- 已确认根因是 `Flyway 10` 缺少 MySQL 数据库支持模块；已在 `backend/pom.xml` 增加 `org.flywaydb:flyway-mysql`

## Verification Conclusion
- 当前根因已收敛为两层：
- 第一层：先前 Docker Desktop 未启动，已通过用户操作排除
- 第二层：Docker 镜像源问题已绕过，当前实际阻塞点为 `backend/pom.xml` 引用了 Maven Central 中不存在的 `flyway-database-h2:10.20.1`
