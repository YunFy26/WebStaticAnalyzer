export default {
  // Sidebar
  nav: {
    project: 'Project',
    analysis: 'Analysis',
    routes: 'Route Info',
    callgraph: 'Call Graph',
    taint: 'Taint Analysis'
  },

  // Project page
  project: {
    title: 'Project Management',
    subtitle: 'Upload and manage Spring Web application projects',
    upload: 'Upload Project',
    totalFiles: 'Total Files',
    javaClasses: 'Java Classes',
    configFiles: 'Config Files',
    structure: 'Project Structure',
    loaded: 'Loaded',
    awaiting: 'Awaiting Upload',
    emptyTip: 'Please upload a Spring Web application project (.jar / .zip)',
    loadSuccess: 'Project loaded successfully'
  },

  // Analysis page
  analysis: {
    title: 'Analysis Control Panel',
    subtitle: 'Configure and execute static analysis tasks',
    pipeline: 'Analysis Pipeline',
    step1: 'Context Parsing',
    step1Desc: 'Load rules & build app context',
    step2: 'Call Graph',
    step2Desc: 'Entry, DI, AOP modeling',
    step3: 'Taint Rule Gen',
    step3Desc: 'Source & Sink auto-generation',
    step4: 'Taint Analysis',
    step4Desc: 'Execute taint flow detection',
    fullAnalysis: 'Full Analysis',
    fullBtn: 'Run Complete Analysis Pipeline',
    fullRunning: 'Full Analysis Running...',
    log: 'Analysis Log',
    completed: 'Completed',
    running: 'Running...',
    // Modules
    mod1Title: 'Spring Context Parsing',
    mod1Desc: 'Load annotation rules, scan classes, parse XML & MyBatis mappings',
    mod1Btn: 'Run Context Parsing',
    mod1Tags: ['Annotation Rules', 'XML Config', 'MyBatis Mapping'],
    mod2Title: 'Call Graph Enhancement',
    mod2Desc: 'Entry point construction, DI modeling, AOP weaving',
    mod2Btn: 'Build Call Graph',
    mod2Tags: ['Entry Points', 'Dependency Injection', 'AOP Advice'],
    mod3Title: 'Taint Rule Generation',
    mod3Desc: 'Auto-generate Source configs from entries and Sink configs from MyBatis',
    mod3Btn: 'Generate Taint Rules',
    mod3Tags: ['Source Extraction', 'Sink Detection', 'Risk Filtering'],
    mod4Title: 'Taint Flow Analysis',
    mod4Desc: 'Execute Tai-e taint analysis with generated configurations',
    mod4Btn: 'Run Taint Analysis',
    mod4Tags: ['Taint Propagation', 'Vulnerability Detection', 'Path Tracing'],
    // Log messages
    logStarting: 'Starting',
    logCompleted: 'Completed',
    logFullStart: 'Starting full analysis pipeline...',
    logFullDone: 'Full analysis pipeline completed successfully!'
  },

  // Routes page
  routes: {
    title: 'Route Information',
    subtitle: 'Spring MVC router analysis results',
    searchPlaceholder: 'Search URL or method name...',
    httpMethod: 'HTTP Method',
    controller: 'Controller',
    urlPattern: 'URL Pattern',
    http: 'HTTP',
    method: 'Method',
    parameters: 'Parameters',
    callGraph: 'Call Graph',
    view: 'View',
    routes: 'routes'
  },

  // Call Graph page
  callgraph: {
    title: 'Call Graph Visualization',
    subtitle: 'View call flow graphs for each entry method (from output/callFlows/)',
    selectPlaceholder: 'Select an entry method...',
    downloadSvg: 'Download SVG',
    dotSource: 'DOT Source',
    hide: 'Hide',
    show: 'Show',
    emptyTip: 'Select an entry method to view its call graph'
  },

  // Taint page
  taint: {
    title: 'Taint Analysis Results',
    subtitle: 'Generated Source/Sink configurations and detected taint flow paths',
    sourceConfig: 'Source Config',
    sinkConfig: 'Sink Config',
    taintFlows: 'Taint Flows',
    flowGraph: 'Flow Graph',
    sourceDesc: 'Auto-generated from controller entry parameters (output/taint-config-generated.yml)',
    sinkDesc: 'Auto-generated from MyBatis mapper methods using ${...} text concatenation',
    flowDesc: 'Detected taint propagation paths from Source to Sink (output/taint-flow-graph.dot)',
    graphDesc: 'Taint flow graph visualization (rendered from taint-flow-graph.dot)',
    kind: 'Kind',
    method: 'Method',
    index: 'Index',
    type: 'Type',
    risk: 'Risk',
    sqlFragment: 'SQL Fragment'
  }
}
