---
name: "api-debug-and-integration"
description: "Handles API联调, request tracing, auth debugging, and end-to-end verification. Invoke when frontend and backend disagree or an interface returns unexpected status codes."
---

# 接口联调与排障 Skill

用于处理前后端联调、请求链路分析、认证失败、跨域问题、状态码异常和接口验收。

## 适用场景

当出现以下情况时调用：

- 页面请求返回 `401`、`403`、`404`、`500`
- 前端页面正常，但接口数据异常
- 浏览器请求和后端日志不一致
- 需要核对 `nginx`、代理、鉴权头、跨域、路径重写
- 需要形成可复现、可验证的联调结论

## 排查顺序

1. 确认请求地址、方法、参数、请求头
2. 判断问题出在浏览器、网关、前端、后端还是数据库
3. 对比直连后端和经代理访问的差异
4. 观察响应头、响应体、状态码和日志
5. 验证修复前后行为是否一致

## 重点证据

- 浏览器 Network 中的请求与响应
- `curl` 或 `.http` 请求的复现实验
- 后端日志、反向代理日志、健康检查状态
- Token、Origin、Cookie、Authorization 头
- 容器内访问与宿主机访问的对比结果

## 常见问题模型

- 路径前缀不一致
- 旧 token 干扰新登录
- 代理转发了不该保留的请求头
- CORS 白名单未覆盖当前地址
- 直连可用但经网关不可用

## 输出要求

- 清晰描述“在哪一层失败”
- 给出最小复现命令
- 给出最小修复点
- 给出修复后的验证命令
