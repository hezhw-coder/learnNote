---
name: "docker-deploy-and-troubleshooting"
description: "Builds, runs, and diagnoses containerized apps and local deployment issues. Invoke when working with Dockerfiles, Compose, ports, env vars, health checks, or runtime failures."
---

# Docker 部署与排障 Skill

用于处理 `Dockerfile`、`docker compose`、环境变量、容器健康检查、端口映射、反向代理和本地部署问题。

## 适用场景

当出现以下情况时调用：

- 项目需要容器化部署
- 容器能启动但服务不可访问
- 构建镜像失败、依赖拉取失败、健康检查失败
- 需要调整端口、网关、代理、环境变量或卷挂载
- 需要编写部署步骤和故障排查文档

## 推荐工作流

1. 核对 `Dockerfile` 与 `compose` 结构
2. 确认构建产物、依赖、镜像来源
3. 核对端口映射、网络、依赖顺序、健康检查
4. 对比容器内访问与宿主机访问差异
5. 更新部署命令、环境变量和文档

## 核心检查项

- 容器是否真正暴露端口
- 健康检查是否可达
- 初始化服务是否完成
- 反向代理是否正确转发路径与请求头
- 是否存在代理、WSL、回环或本机安全软件干扰

## 常见问题模型

- 镜像源不可用
- 宿主机端口冲突
- 容器内服务正常但外部不可访问
- 环境变量缺失导致启动失败
- 数据库未初始化导致后端启动失败

## 输出要求

- 给出启动命令、状态检查命令、日志命令
- 清晰说明“构建失败”“启动失败”“访问失败”分别怎么查
- 文档中明确访问地址、默认账号和常见故障处理步骤
