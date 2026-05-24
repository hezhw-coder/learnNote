# 医疗数据中心前端

## 技术栈

- Vite
- Vue 3
- TypeScript
- Vue Router
- Pinia
- Element Plus
- ECharts

## 启动方式

```bash
npm install
npm run dev
```

## 当前说明

- 当前骨架默认使用 `src/api/mock.ts` 提供页面演示数据。
- 联调时可保留 `src/api/http.ts` 作为统一请求入口，并逐步将模块 API 切换到真实后端接口。
- 推荐后端统一响应格式使用计划文档中的 `{ code, message, data, requestId, timestamp }`。
