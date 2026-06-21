# 项目交接文档 --- Flink 实时推荐系统

## 一、项目概览

> **项目路径**: D:\\idea2024\\Flink_Recommend\\flink-recommend
> **GitHub**: https://github.com/Estralla-extra/flink-recommend
> **分支**: main

一个基于 Flink + Redis + Kafka 的实时推荐系统，前端为 Vue 3 + Vite，后端为 Spring Boot。

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
- **所有源文件使用 LF (\\\\n) 行尾，不是 CRLF (\\\\r\\\\n)**
- **写入含中文的文件时必须使用 UTF-8 编码（无 BOM）**
- PowerShell 的 @'...'@ heredocs 会对中文编码造成破坏，不要用
- 推荐使用 [System.IO.File]::WriteAllText() 配合 UTF8Encoding 写入文件
- 写入时记得 $ 符号需要进行转义

### 2.2 Git 权限
- **当前 Shell 运行的用户是 CodexSandboxOffline，但 .git 目录归属于 Esralla**
- **不要在 sandbox 中运行 git 命令** --- 会因权限冲突报错
- 需要 Git 操作时，只告诉用户运行命令，不要自己执行

### 2.3 编译与运行
`
cd D:\\idea2024\\Flink_Recommend\\flink-recommend\\recommend-ui
npx vite build
`

后端：IntelliJ 中运行 RecommendWebApplication.java，访问 http://localhost:8080
Flink：运行 RealtimeRecommendJob.java

---

## 三、当前状态

### 已完成的工作

1. **导航栏** --- 顶部导航已拆分为 4 项：监控大屏 / 用户推荐 / 商品推荐 / 深度分析
2. **用户推荐页** --- 移除了 Tab 切换逻辑，只保留用户模式（输入用户ID -> Flink 3分钟窗口推荐 + 热门兜底）
3. **商品推荐页** --- 全新页面（输入商品ID -> 实时相似商品 + 热门兜底）
4. **深度分析页** --- 副标题改为左上角面包屑样式：**深度分析** / 小时热力图 ... 环节事件数对比 ... 品类分布
5. **构建通过** --- 最近一次 npx vite build 成功（674 modules，1.79s）

### 待完成 / 用户提及过的方向

1. **个人用户界面** --- 用户信息页面、退出功能完善
2. **注册页面完善** --- 当前注册可能不完整
3. **前端科技风 UI** --- 用户之前提到想要科技风（粒子背景 + 发光字体 + 扫描线）
4. **商品推荐页联动状态** --- Flink窗口缓存 / 实时相似召回 / 热门兜底 三路状态展示

### 已知问题
- 深度分析页面底部状态栏之前被改过，密集程度需调整
- 热力图偶尔只显示 BUY 数据（因心跳事件过滤逻辑）

---

## 四、关键文件索引

### 前端（recommend-ui/src/）

| 文件 | 用途 |
|------|------|
| views/Dashboard.vue | 监控大屏主页面 |
| views/Analytics.vue | 深度分析页面 |
| views/Recommend.vue | 用户推荐（包装 UserRecommend） |
| views/ItemRecommend.vue | **商品推荐（新建）** |
| views/Login.vue | 登录页 |
| views/Register.vue | 注册页 |
| components/UserRecommend.vue | 用户推荐逻辑组件 |
| components/ItemRecommend.vue | **商品推荐逻辑组件（新建）** |
| components/HeaderBar.vue | 顶部导航栏 |
| components/ParticleBg.vue | 粒子背景（全局使用） |
| components/charts/HeatmapChart.vue | 热力图 |
| components/charts/FunnelChart.vue | 转化漏斗图 |
| components/charts/CategoryPie.vue | 品类分布图 |
| router/index.ts | 路由定义 |
| stores/recommend.ts | 推荐查询状态管理（用户+商品共用 Store） |
| api/index.ts | API 调用封装 |
| types/index.ts | TypeScript 类型定义 |

### 后端（recommend-web/src/main/java/com/recommend/web/）

| 文件 | 用途 |
|------|------|
| controller/RecommendController.java | 推荐 API |
| controller/AnalyticsController.java | 深度分析 API |
| controller/RealtimeMetricsController.java | 实时指标 API |

### Flink（flink-job/src/main/java/com/recommend/flink/）

| 文件 | 用途 |
|------|------|
| RealtimeRecommendJob.java | Flink 主作业（BehaviorParser 内有行为过滤） |
| DataProducer.java | Kafka 数据生产者 |

---

## 五、数据流与架构

`
Kafka (用户行为事件)
  |
  v
Flink RealtimeRecommendJob
  |-- 3分钟窗口聚合 --> Redis (窗口缓存)
  |-- 实时相似召回 --> Redis (sim:*)
  |-- 热门兜底 --> Redis (hot:items)
  |
  v
Spring Boot API (recommend-web)
  |-- /api/recommend --> 读取 Redis 返回推荐结果
  |-- /api/analytics/* --> 热力图/漏斗/品类数据
  |-- /api/metrics/* --> 系统指标
  |
  v
Vue 前端 (recommend-ui) --> 用户界面
`

### 推荐查询逻辑（stores/recommend.ts）
- fetchForUser(userId, itemId?) 是通用的查询方法
- 用户推荐：调用 fetchForUser("用户ID")
- 商品推荐：调用 fetchForUser("__item__", "商品ID")（固定字符串 __item__ 占位 userId）
- 返回数据包含 sourceLists: { flink, realtime, hot } 三路结果

---

## 六、开发提示

1. **修改 Vue 文件后** -> 需要 npx vite build 重新构建前端
2. **构建产物**输出到 recommend-web/src/main/resources/static/
3. **重启后端** -> 运行 RecommendWebApplication.main()
4. **Flink 作业重启** -> 运行 RealtimeRecommendJob.main()
5. **提交代码** -> 用户在终端执行：
   `
   cd D:\\idea2024\\Flink_Recommend\\flink-recommend
   git add -A
   git commit -m "commit message"
   git push
   `
