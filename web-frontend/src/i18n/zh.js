export default {
  // Sidebar
  nav: {
    project: '项目管理',
    analysis: '分析控制',
    routes: '路由信息',
    callgraph: '调用图',
    taint: '污点分析'
  },

  // Project page
  project: {
    title: '项目管理',
    subtitle: '上传和管理 Spring Web 应用项目',
    upload: '上传项目',
    totalFiles: '总文件数',
    javaClasses: 'Java 类',
    configFiles: '配置文件',
    structure: '项目结构',
    loaded: '已加载',
    awaiting: '等待上传',
    emptyTip: '请上传 Spring Web 应用项目（.jar / .zip）',
    loadSuccess: '项目加载成功'
  },

  // Analysis page
  analysis: {
    title: '分析控制面板',
    subtitle: '配置并执行静态分析任务',
    pipeline: '分析流水线',
    step1: '上下文解析',
    step1Desc: '加载规则并构建应用上下文',
    step2: '调用图构建',
    step2Desc: '入口点、依赖注入、AOP建模',
    step3: '污点规则生成',
    step3Desc: 'Source 与 Sink 自动生成',
    step4: '污点分析',
    step4Desc: '执行污点流检测',
    fullAnalysis: '完整分析',
    fullBtn: '运行完整分析流水线',
    fullRunning: '完整分析运行中...',
    log: '分析日志',
    completed: '已完成',
    running: '运行中...',
    // Modules
    mod1Title: 'Spring 上下文解析',
    mod1Desc: '加载注解规则、扫描类、解析 XML 与 MyBatis 映射',
    mod1Btn: '运行上下文解析',
    mod1Tags: ['注解规则', 'XML 配置', 'MyBatis 映射'],
    mod2Title: '调用图增强分析',
    mod2Desc: '入口点构建、依赖注入建模、切面行为建模',
    mod2Btn: '构建调用图',
    mod2Tags: ['入口点', '依赖注入', 'AOP 通知'],
    mod3Title: '污点规则自动生成',
    mod3Desc: '从入口方法自动生成 Source 配置，从 MyBatis 自动生成 Sink 配置',
    mod3Btn: '生成污点规则',
    mod3Tags: ['Source 提取', 'Sink 检测', '风险过滤'],
    mod4Title: '污点流分析',
    mod4Desc: '使用生成的配置执行 Tai-e 污点分析',
    mod4Btn: '运行污点分析',
    mod4Tags: ['污点传播', '漏洞检测', '路径追踪'],
    // Log messages
    logStarting: '正在启动',
    logCompleted: '已完成',
    logFullStart: '正在启动完整分析流水线...',
    logFullDone: '完整分析流水线执行成功！'
  },

  // Routes page
  routes: {
    title: '路由信息',
    subtitle: 'Spring MVC 路由分析结果',
    searchPlaceholder: '搜索 URL 或方法名...',
    httpMethod: 'HTTP 方法',
    controller: '控制器',
    urlPattern: 'URL 模式',
    http: 'HTTP',
    method: '方法名',
    parameters: '参数',
    callGraph: '调用图',
    view: '查看',
    routes: '个路由'
  },

  // Call Graph page
  callgraph: {
    title: '调用图可视化',
    subtitle: '查看每个入口方法的调用流图（来自 output/callFlows/）',
    selectPlaceholder: '选择一个入口方法...',
    downloadSvg: '下载 SVG',
    dotSource: 'DOT 源码',
    hide: '隐藏',
    show: '显示',
    emptyTip: '请选择一个入口方法以查看其调用图'
  },

  // Taint page
  taint: {
    title: '污点分析结果',
    subtitle: '生成的 Source/Sink 配置与检测到的污点传播路径',
    sourceConfig: 'Source 配置',
    sinkConfig: 'Sink 配置',
    taintFlows: '污点传播路径',
    flowGraph: '流图可视化',
    sourceDesc: '从控制器入口参数自动生成（output/taint-config-generated.yml）',
    sinkDesc: '从 MyBatis 映射方法中使用 ${...} 文本拼接自动生成',
    flowDesc: '检测到的从 Source 到 Sink 的污点传播路径（output/taint-flow-graph.dot）',
    graphDesc: '污点流图可视化（从 taint-flow-graph.dot 渲染）',
    kind: '类型',
    method: '方法',
    index: '索引',
    type: '参数类型',
    risk: '风险',
    sqlFragment: 'SQL 片段'
  }
}
