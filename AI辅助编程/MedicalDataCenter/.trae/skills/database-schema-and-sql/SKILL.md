---
name: "database-schema-and-sql"
description: "Designs schema changes, migrations, queries, and data troubleshooting. Invoke when tasks involve tables, indexes, Flyway, SQL optimization, or data compatibility."
---

# 数据库建模与 SQL Skill

用于处理数据库表结构、迁移脚本、初始化数据、查询语句、索引设计和兼容性问题。

## 适用场景

当出现以下情况时调用：

- 需要新增或修改表结构
- 需要编写 `Flyway`、`Liquibase` 或初始化 SQL
- 需要排查数据库启动失败、迁移失败、兼容性问题
- 需要优化查询、索引、分页、统计或数据清洗逻辑

## 工作重点

- 明确当前数据库类型和方言
- 区分开发环境与 Docker/生产环境差异
- 确认建表、迁移和初始化数据是否可重复执行
- 检查索引、唯一约束、外键和空值语义

## 推荐工作流

1. 确认数据库类型与版本
2. 审查现有表结构和迁移顺序
3. 设计最小变更方案
4. 处理兼容性与回滚策略
5. 给出验证 SQL 或运行步骤

## 常见风险

- H2 与 MySQL/TiDB 语法不兼容
- 初始化脚本不可重复执行
- 默认值、时间字段、主键策略不一致
- 索引缺失导致列表或搜索性能差
- 数据清洗规则与业务含义不一致

## 输出要求

- 明确说明影响的表、字段、索引、脚本
- 说明变更原因与兼容性考虑
- 给出验证语句或最小验证路径
