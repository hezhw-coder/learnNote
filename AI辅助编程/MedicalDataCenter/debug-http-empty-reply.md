[OPEN] HTTP empty reply debugging

## Session
- session_id: http-empty-reply
- symptom: 浏览器和 `curl` 访问 `http://127.0.0.1/` 与 `http://127.0.0.1:8080/actuator/health` 都返回 `Empty reply from server`
- expected: 前端首页可访问，后端健康检查返回 JSON

## Runtime Evidence
- `docker compose ps` 显示 `mdc-backend` 为 `Up (healthy/starting)`，`mdc-frontend` 为 `Up`
- `docker inspect ... PortBindings` 显示：
  - `frontend` 绑定 `80/tcp -> HostPort 80`
  - `backend` 绑定 `8080/tcp -> HostPort 8080`
- `curl.exe -v http://127.0.0.1/` 已连接到 `127.0.0.1:80`，随后返回 `Empty reply from server`
- `curl.exe -v http://127.0.0.1:8080/actuator/health` 已连接到 `127.0.0.1:8080`，随后返回 `Empty reply from server`
- `docker port mdc-frontend` 与 `docker port mdc-backend` 为空
- `tasklist /FI "PID eq 10912"` 显示端口监听者是 `com.docker.backend.exe`
- `tasklist /FI "PID eq 30512"` 显示 `::1` 监听者是 `wslrelay.exe`
- `frontend` 日志显示 `nginx` 已正常启动，无崩溃迹象
- `backend` 日志显示 Spring Boot、Flyway、Tomcat 均成功启动，并输出 `Started MedicalDataCenterBackendApplication`
- `backend` 日志中的 `dispatcherServlet` 初始化更可能来自容器内健康检查，而非宿主机 `curl`
- `docker exec mdc-backend wget -qO- http://127.0.0.1:8080/actuator/health` 返回 `{"status":"UP"}`
- `docker exec mdc-frontend wget -qO- http://127.0.0.1/` 返回完整首页 HTML
- `docker info` 显示当前运行在 `desktop-linux` 上，且存在全局 `HTTP Proxy` / `HTTPS Proxy`

## Hypotheses
1. Docker Desktop 端口代理层异常，能建立 TCP 连接，但未把 HTTP 请求正确转发到容器。
2. 宿主机回环请求被本机代理、安全软件或系统组件拦截，导致 Docker 代理提前断开响应。
3. `docker port` 为空说明发布状态与容器配置不一致，Docker 运行态端口发布存在异常。
4. 容器内服务本身实际可用，但问题仅发生在宿主机访问已发布端口时。

## Hypothesis Status
- confirmed-likely:
  - 4. 容器内服务本身实际可用
  - 1. Docker Desktop 宿主机端口转发链路异常
- still-possible:
  - 2. 本机代理/安全环境干扰 Docker Desktop 转发
  - 3. Docker 运行态端口发布信息异常
- rejected:
  - 前后端应用未启动或业务代码导致 HTTP 空响应

## Next Evidence To Collect
- Docker Desktop 重启后的宿主机访问结果
- 关闭 Docker Desktop 代理后的宿主机访问结果
- 如需规避当前环境问题，可尝试改用高位宿主机端口再验证

## Latest Action
- 根据用户选择，已将宿主机映射改为高位端口：
  - `frontend`: `18081 -> 80`
  - `backend`: `18080 -> 8080`
- 目的：绕过当前 `80/8080` 上疑似异常的 Docker Desktop 宿主机发布链路
- 新症状：前端页面已可访问，但点击登录返回 `403`
- 静态证据显示前端会对所有请求统一附带本地 `mdc-token`，包括 `/auth/login`
- 已做最小修复：登录请求不再附带旧 `Authorization` 头，避免残留失效 token 干扰登录接口
- 新证据：`/api/auth/login` 的 `403` 响应头包含 `Vary: Origin` 等 CORS 相关头，且由 Spring Security 风格返回
- 已确认更强根因：docker 环境未放行 `http://127.0.0.1:18081` / `http://localhost:18081`，登录 POST 经 nginx 反代到 backend:8080 时触发 CORS 拒绝
- 已修复：在 `application-docker.yml` 中补充 docker 场景允许来源
- 新的对比证据：
  - 直连后端并带 `Origin: http://127.0.0.1:18081` 时，明确返回 `403 Invalid CORS request`
  - 这说明阻塞点就是 `Origin` 触发的后端 CORS 校验
- 已追加更精准修复：在 `nginx` 反代 `/api`、`/open-api`、`/actuator` 时移除 `Origin` 头，避免同源前端场景误触发后端 CORS

## Status
- waiting_for_user_evidence: true
