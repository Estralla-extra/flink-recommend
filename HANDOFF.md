# 项目交接文档 --- Flink 实时推荐系统

## 一、项目概览
> **项目路径**: D:\idea2024\Flink_Recommend\flink-recommend
> **GitHub**: https://github.com/Estralla-extra/flink-recommend
> **分支**: main

一个基于 Flink + Redis + Kafka + MySQL 的实时推荐系统，前端为 Vue 3 + Vite，后端为 Spring Boot。

### 模块结构

| 目录 | 用途 |
|------|------|
| flink-job/ | Flink 实时计算作业（Java） |
| recommend-web/ | Spring Boot 后端（端口 8080，提供 REST API + 静态前端） |
| recommend-ui/ | Vue 3 + Vite 前端（通过 npx vite build 构建输出到 recommend-web/src/main/resources/static/） |

### 页面路由

| 路由 | 页面 | 说明 |
|------|------|------|
| /dashboard | 监控大屏 | 主监控页面 |
| /recommend | 用户推荐 | 用户ID -> Flink 3分钟窗口推荐 + 热门兜底 |
| /item-recommend | **商品推荐** | 商品ID -> 实时相似商品 + 热门兜底 |
| /analytics | 深度分析 | 热力图 + 漏斗 + 品类分布 |
| /login | 登录 | |
| /register | 注册 | |

---

## 二、关键技术约束（重要！）

### 2.1 编码规范
- **所有源文件使用 LF (\n) 行尾，不是 CRLF (\r\n)**
- **写入含中文的文件时必须使用 UTF-8 编码（无 BOM）**
- PowerShell 的 @'...'@ heredocs 会对中文编码造成破坏，不要用
- 推荐使用 [System.IO.File]::WriteAllText() 配合 UTF8Encoding 写入文件
- 写入时记得 $ 符号需要进行转义

### 2.2 Git 权限
- **当前 Shell 运行的用户是 CodexSandboxOffline，但 .git 目录归属于 Esralla**
- **不要在 sandbox 中运行 git 命令** --- 会因权限冲突报错
- 需要 Git 操作时，只告诉用户运行命令，不要自己执行

### 2.3 编译与运行
```
cd D:\idea2024\Flink_Recommend\flink-recommend\recommend-ui
npx vite build
```

后端：IntelliJ 中运行 RecommendWebApplication.java，访问 http://localhost:8080
Flink：运行 RealtimeRecommendJob.java

---

## 三、当前状态

### 已完成的工作

1. **导航栏** --- 顶部导航已拆分为 4 项：监控大屏 / 用户推荐 / 商品推荐 / 深度分析
2. **用户推荐页** --- 移除了 Tab 切换逻辑，只保留用户模式
3. **商品推荐页** --- 全新页面（输入商品ID -> 实时相似商品 + 热门兜底）
4. **深度分析页** --- 面包屑样式副标题
5. **构建通过** --- npx vite build 成功
6. **前端科技风 UI** --- 粒子背景 + 发光字体 + 扫描线动画 + 渐变面板 + D3 动效
7. **商品推荐页联动状态** --- 实时相似召回 + 热门兜底两路状态卡片
8. **底部状态栏密度优化** --- 6 项指标均匀 flex 分布
9. **热力图心跳过滤修复** --- Flink 端和前端均过滤 heartbeat 事件
10. **深度分析页日期/小时选择** --- 日期选择器 + 小时选择器（仅影响漏斗图）
11. **Redis TTL 过期策略** --- 所有 Redis 写入均设 expire
12. **MySQL 数据归档** --- 5 张分析表（analytics_funnel/heatmap/category_hot/kpi/user_activity）
13. **每日定时迁移** --- 每天 00:05 Redis 数据写入 MySQL
14. **API Redis-first + MySQL 兜底** --- heatmap/category/user_activity/funnel
15. **Future-date 校验** --- 后端所有分析接口校验未来日期
16. **品类图日期修复** --- fetchCategoryTop 加 ?date= 参数
17. **活跃度趋势日期跟随** --- ActivityTrend 接收 dateKey prop
18. **.catch() 错误处理修复** --- Promise.all 中 .catch() 优先级 bug
19. **todayStr 动态化** --- 用 td() 函数替代固定 const now
20. **趋势图 tooltip 自适应 + 彩色** --- 自动计算尺寸 + 颜色编码
21. **空状态区分** --- 今天/历史日期显示不同提示
22. **脉动条紫色 + 4s** --- #8b5cf6，速度 4s
23. **热力图 X 轴每4小时** --- 00/04/08/12/16/20
24. **状态标签字体优化** --- 字号放大、字重减轻

### 待完成 / 用户提及过的方向

1. **用户登录/注册功能完善** --- 当前 AuthController 为 Memory 级别，需对接 MySQL 持久化
2. **个人用户界面** --- 用户信息页面、退出功能完善

---

## 四、MySQL 数据库

### 连接信息
- 主机: 192.168.249.102:3306
- 库名: flink_recommend
- 用户: recommend_app
- 密码: Recommend@2026

### 表结构

| 表名 | 用途 |
|------|------|
| analytics_funnel | 漏斗数据 |
| analytics_heatmap | 热力图数据 |
| analytics_category_hot | 品类热度 |
| analytics_kpi | 每日KPI |
| analytics_user_activity | 用户活跃度 |

### 定时迁移
- 时间: 每天 00:05（DataMigrationService）
- 逻辑: 读取 Redis 前一天数据，upsert 到 MySQL

---

## 五、关键文件索引

### 前端（recommend-ui/src/）

| 文件 | 用途 |
|------|------|
| views/Analytics.vue | 深度分析（日期/小时选择） |
| components/charts/ActivityTrend.vue | 活跃度趋势（tooltip 自适应 + 颜色编码） |
| components/charts/HeatmapChart.vue | 热力图（X轴4小时） |
| api/index.ts | fetchCategoryTop 支持 date 参数 |

### 后端（recommend-web/src/main/java/com/recommend/web/）

| 文件 | 用途 |
|------|------|
| controller/AnalyticsController.java | Redis-first + MySQL 兜底 |
| dao/AnalyticsDao.java | MySQL 数据访问 |
| service/DataMigrationService.java | 每日 00:05 定时迁移 |

### Flink（flink-job/src/main/java/com/recommend/flink/）

| 文件 | 用途 |
|------|------|
| RealtimeRecommendJob.java | Flink 主作业（TTL + 品类日期后缀） |

---

## 六、数据流与架构

```
Kafka (用户行为事件)
  |
  v
Flink RealtimeRecommendJob (TTL 过期 + 品类日期后缀)
  |-- 写入 Redis (3天/7天 TTL)
  |
  ├── Spring Boot API (Redis-first + MySQL 兜底)
  │   └── /api/analytics/*
  │
  ├── DataMigrationService (每日 00:05)
  │   └── Redis --> MySQL
  │
  └── Vue 前端 --> 用户界面
```

---

## 七、开发提示

1. **修改 Vue 文件后** -> 需要 npx vite build 重新构建前端
2. **构建产物**输出到 recommend-web/src/main/resources/static/
3. **重启后端** -> 运行 RecommendWebApplication.main()
4. **Flink 作业重启** -> 运行 RealtimeRecommendJob.main()
5. **MySQL 建表** -> 已有 5 张表，新功能加表请同步更新
6. **提交代码** -> 用户在终端执行：
   ```
   cd D:\idea2024\Flink_Recommend\flink-recommend
   git add -A
   git commit -m "commit message"
   git push
   ```
