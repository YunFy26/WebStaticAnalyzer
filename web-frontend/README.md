# WebAnalyzer Frontend

WebAnalyzer 前端界面 —— 面向 Spring Web 应用的静态安全分析系统。

## 技术栈

- **Vue 3** + **Vite**
- **Element Plus** — UI 组件库
- **Vue Router 4** — 路由管理
- **Viz.js** — Graphviz DOT 调用图渲染
- 支持 **中文 / English** 双语切换

## 环境要求

- Node.js >= 18
- npm >= 9

## 启动方式

```bash
# 1. 进入前端目录
cd web-frontend

# 2. 安装依赖
npm install

# 3. 启动开发服务器
npm run dev
```

启动后访问 http://localhost:5173

## 生产构建

```bash
# 构建生产版本
npm run build

# 本地预览生产构建
npm run preview
```

构建产物输出在 `dist/` 目录下。

## 功能概览

| 页面 | 功能说明 |
|------|---------|
| 项目管理 | 上传 Spring Web 应用项目（.jar / .war / .zip），展示项目结构树 |
| 分析控制 | 四步分析流水线（上下文解析 → 调用图构建 → 污点规则生成 → 污点分析） |
| 路由信息 | 展示 Controller 路由映射，支持按 URL、HTTP 方法、控制器筛选 |
| 调用图 | 可视化渲染每个入口方法的调用流图（DOT → SVG），支持缩放与下载 |
| 污点分析 | 展示 Source/Sink 配置、污点传播路径（Source → Transfer → Sink）及流图可视化 |

## 项目结构

```
src/
├── i18n/                # 国际化（中英文）
│   ├── index.js         # useI18n composable
│   ├── zh.js            # 中文
│   └── en.js            # English
├── data/
│   └── demo.js          # 演示数据
├── router/
│   └── index.js         # 路由配置
├── views/
│   ├── ProjectView.vue  # 项目管理
│   ├── AnalysisView.vue # 分析控制面板
│   ├── RoutesView.vue   # 路由信息
│   ├── CallGraphView.vue# 调用图可视化
│   └── TaintView.vue    # 污点分析结果
└── App.vue              # 主布局 + 语言切换
```
