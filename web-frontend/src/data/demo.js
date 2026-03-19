// Demo data simulating WebAnalyzer output

export const projectTree = [
  {
    label: 'jeecg-boot-3.5.3',
    children: [
      {
        label: 'src/main/java',
        children: [
          {
            label: 'org.jeecg.modules.demo.test.controller',
            children: [
              { label: 'JeecgOrderErpMainController.java' },
              { label: 'JeecgOrderMainController.java' },
              { label: 'JeecgDemoController.java' },
              { label: 'JoaDemoController.java' },
              { label: 'JeecgDynamicDataController.java' }
            ]
          },
          {
            label: 'org.jeecg.modules.demo.mock.vxe.controller',
            children: [
              { label: 'VxeMockController.java' }
            ]
          },
          {
            label: 'org.jeecg.modules.demo.mock',
            children: [
              { label: 'MockController.java' }
            ]
          },
          {
            label: 'org.jeecg.modules.system.controller',
            children: [
              { label: 'SysUserController.java' },
              { label: 'SysRoleController.java' },
              { label: 'SysDictController.java' },
              { label: 'SysPermissionController.java' },
              { label: 'SysDepartController.java' },
              { label: 'LoginController.java' }
            ]
          },
          {
            label: 'org.jeecg.modules.system.service',
            children: [
              { label: 'ISysUserService.java' },
              { label: 'impl/SysUserServiceImpl.java' },
              { label: 'ISysRoleService.java' },
              { label: 'impl/SysRoleServiceImpl.java' }
            ]
          },
          {
            label: 'org.jeecg.modules.system.mapper',
            children: [
              { label: 'SysUserMapper.java' },
              { label: 'SysRoleMapper.java' },
              { label: 'SysDictMapper.java' }
            ]
          },
          {
            label: 'org.jeecg.common.aspect',
            children: [
              { label: 'AutoLogAspect.java' },
              { label: 'DictAspect.java' },
              { label: 'PermissionDataAspect.java' }
            ]
          }
        ]
      },
      {
        label: 'src/main/resources',
        children: [
          { label: 'application.yml' },
          {
            label: 'mapper',
            children: [
              { label: 'SysUserMapper.xml' },
              { label: 'SysRoleMapper.xml' },
              { label: 'SysDictMapper.xml' }
            ]
          }
        ]
      },
      { label: 'pom.xml' }
    ]
  }
]

export const controllerRoutes = [
  {
    className: 'org.jeecg.modules.system.controller.SysUserController',
    simpleName: 'SysUserController',
    baseUrls: ['/sys/user'],
    methods: [
      { name: 'queryPageList', httpMethod: 'GET', urls: ['/sys/user/list'], params: ['SysUser', 'Integer', 'Integer', 'HttpServletRequest'], signature: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result queryPageList(org.jeecg.modules.system.entity.SysUser,java.lang.Integer,java.lang.Integer,javax.servlet.http.HttpServletRequest)>' },
      { name: 'add', httpMethod: 'POST', urls: ['/sys/user/add'], params: ['JSONObject'], signature: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result add(com.alibaba.fastjson.JSONObject)>' },
      { name: 'edit', httpMethod: 'PUT', urls: ['/sys/user/edit'], params: ['JSONObject'], signature: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result edit(com.alibaba.fastjson.JSONObject)>' },
      { name: 'delete', httpMethod: 'DELETE', urls: ['/sys/user/delete'], params: ['String'], signature: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result delete(java.lang.String)>' },
      { name: 'deleteBatch', httpMethod: 'DELETE', urls: ['/sys/user/deleteBatch'], params: ['String'], signature: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result deleteBatch(java.lang.String)>' },
      { name: 'queryById', httpMethod: 'GET', urls: ['/sys/user/queryById'], params: ['String'], signature: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result queryById(java.lang.String)>' },
      { name: 'changePassword', httpMethod: 'PUT', urls: ['/sys/user/changePassword'], params: ['SysUser'], signature: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result changePassword(org.jeecg.modules.system.entity.SysUser)>' },
      { name: 'frozenBatch', httpMethod: 'PUT', urls: ['/sys/user/frozenBatch'], params: ['String', 'String'], signature: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result frozenBatch(java.lang.String,java.lang.String)>' }
    ]
  },
  {
    className: 'org.jeecg.modules.system.controller.SysRoleController',
    simpleName: 'SysRoleController',
    baseUrls: ['/sys/role'],
    methods: [
      { name: 'queryPageList', httpMethod: 'GET', urls: ['/sys/role/list'], params: ['SysRole', 'Integer', 'Integer', 'HttpServletRequest'], signature: '<org.jeecg.modules.system.controller.SysRoleController: org.jeecg.common.api.vo.Result queryPageList(org.jeecg.modules.system.entity.SysRole,java.lang.Integer,java.lang.Integer,javax.servlet.http.HttpServletRequest)>' },
      { name: 'add', httpMethod: 'POST', urls: ['/sys/role/add'], params: ['SysRole'], signature: '<org.jeecg.modules.system.controller.SysRoleController: org.jeecg.common.api.vo.Result add(org.jeecg.modules.system.entity.SysRole)>' },
      { name: 'edit', httpMethod: 'PUT', urls: ['/sys/role/edit'], params: ['SysRole'], signature: '<org.jeecg.modules.system.controller.SysRoleController: org.jeecg.common.api.vo.Result edit(org.jeecg.modules.system.entity.SysRole)>' },
      { name: 'delete', httpMethod: 'DELETE', urls: ['/sys/role/delete'], params: ['String'], signature: '<org.jeecg.modules.system.controller.SysRoleController: org.jeecg.common.api.vo.Result delete(java.lang.String)>' },
      { name: 'queryAll', httpMethod: 'GET', urls: ['/sys/role/queryall'], params: [], signature: '<org.jeecg.modules.system.controller.SysRoleController: org.jeecg.common.api.vo.Result queryAll()>' }
    ]
  },
  {
    className: 'org.jeecg.modules.system.controller.LoginController',
    simpleName: 'LoginController',
    baseUrls: ['/sys'],
    methods: [
      { name: 'login', httpMethod: 'POST', urls: ['/sys/login'], params: ['SysLoginModel'], signature: '<org.jeecg.modules.system.controller.LoginController: org.jeecg.common.api.vo.Result login(org.jeecg.modules.system.model.SysLoginModel)>' },
      { name: 'logout', httpMethod: 'POST', urls: ['/sys/logout'], params: ['HttpServletRequest', 'HttpServletResponse'], signature: '<org.jeecg.modules.system.controller.LoginController: org.jeecg.common.api.vo.Result logout(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>' },
      { name: 'getEncryptedString', httpMethod: 'GET', urls: ['/sys/getEncryptedString'], params: [], signature: '<org.jeecg.modules.system.controller.LoginController: org.jeecg.common.api.vo.Result getEncryptedString()>' },
      { name: 'checkCaptcha', httpMethod: 'POST', urls: ['/sys/checkCaptcha'], params: ['String', 'String'], signature: '<org.jeecg.modules.system.controller.LoginController: org.jeecg.common.api.vo.Result checkCaptcha(java.lang.String,java.lang.String)>' }
    ]
  },
  {
    className: 'org.jeecg.modules.system.controller.SysDictController',
    simpleName: 'SysDictController',
    baseUrls: ['/sys/dict'],
    methods: [
      { name: 'queryPageList', httpMethod: 'GET', urls: ['/sys/dict/list'], params: ['SysDict', 'Integer', 'Integer', 'HttpServletRequest'], signature: '<org.jeecg.modules.system.controller.SysDictController: org.jeecg.common.api.vo.Result queryPageList(org.jeecg.modules.system.entity.SysDict,java.lang.Integer,java.lang.Integer,javax.servlet.http.HttpServletRequest)>' },
      { name: 'add', httpMethod: 'POST', urls: ['/sys/dict/add'], params: ['SysDict'], signature: '<org.jeecg.modules.system.controller.SysDictController: org.jeecg.common.api.vo.Result add(org.jeecg.modules.system.entity.SysDict)>' },
      { name: 'edit', httpMethod: 'PUT', urls: ['/sys/dict/edit'], params: ['SysDict'], signature: '<org.jeecg.modules.system.controller.SysDictController: org.jeecg.common.api.vo.Result edit(org.jeecg.modules.system.entity.SysDict)>' },
      { name: 'delete', httpMethod: 'DELETE', urls: ['/sys/dict/delete'], params: ['String'], signature: '<org.jeecg.modules.system.controller.SysDictController: org.jeecg.common.api.vo.Result delete(java.lang.String)>' },
      { name: 'queryAllDictItems', httpMethod: 'GET', urls: ['/sys/dict/queryAllDictItems'], params: [], signature: '<org.jeecg.modules.system.controller.SysDictController: org.jeecg.common.api.vo.Result queryAllDictItems()>' }
    ]
  },
  {
    className: 'org.jeecg.modules.demo.test.controller.JeecgDemoController',
    simpleName: 'JeecgDemoController',
    baseUrls: ['/test/jeecgDemo'],
    methods: [
      { name: 'list', httpMethod: 'GET', urls: ['/test/jeecgDemo/list'], params: ['JeecgDemo', 'Integer', 'Integer', 'HttpServletRequest'], signature: '<org.jeecg.modules.demo.test.controller.JeecgDemoController: org.jeecg.common.api.vo.Result list(org.jeecg.modules.demo.test.entity.JeecgDemo,java.lang.Integer,java.lang.Integer,javax.servlet.http.HttpServletRequest)>' },
      { name: 'add', httpMethod: 'POST', urls: ['/test/jeecgDemo/add'], params: ['JeecgDemo'], signature: '<org.jeecg.modules.demo.test.controller.JeecgDemoController: org.jeecg.common.api.vo.Result add(org.jeecg.modules.demo.test.entity.JeecgDemo)>' },
      { name: 'edit', httpMethod: 'PUT', urls: ['/test/jeecgDemo/edit'], params: ['JeecgDemo'], signature: '<org.jeecg.modules.demo.test.controller.JeecgDemoController: org.jeecg.common.api.vo.Result edit(org.jeecg.modules.demo.test.entity.JeecgDemo)>' },
      { name: 'delete', httpMethod: 'DELETE', urls: ['/test/jeecgDemo/delete'], params: ['String'], signature: '<org.jeecg.modules.demo.test.controller.JeecgDemoController: org.jeecg.common.api.vo.Result delete(java.lang.String)>' },
      { name: 'queryById', httpMethod: 'GET', urls: ['/test/jeecgDemo/queryById'], params: ['String'], signature: '<org.jeecg.modules.demo.test.controller.JeecgDemoController: org.jeecg.common.api.vo.Result queryById(java.lang.String)>' },
      { name: 'testOnlineAdd', httpMethod: 'POST', urls: ['/test/jeecgDemo/testOnlineAdd'], params: ['JSONObject'], signature: '<org.jeecg.modules.demo.test.controller.JeecgDemoController: org.jeecg.common.api.vo.Result testOnlineAdd(com.alibaba.fastjson.JSONObject)>' }
    ]
  }
]

// Call graph DOT data for each entry method
export const callGraphs = {
  'SysUserController.queryPageList': `digraph G {
  rankdir=TB;
  ranksep=1.0;
  nodesep=0.5;
  node [shape=box, style=filled, fillcolor="lightblue"];
  edge [color="black"];

  subgraph cluster_legend {
    label="Legend";
    style=dashed;
    fontsize=10;
    rank=source;
    node [shape=plaintext];
    legend [label=<
      <TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">
        <TR><TD BGCOLOR="lightblue"><B>Business Logic</B></TD><TD>Black Solid Line</TD></TR>
        <TR><TD BGCOLOR="lightyellow"><B>AOP Advice</B></TD><TD><FONT COLOR="red">Red Dashed Line (Weaving)</FONT></TD></TR>
        <TR><TD COLSPAN="2"><FONT COLOR="#FF8C00">Orange Solid Line (AOP Internal)</FONT></TD></TR>
      </TABLE>
    >];
  }

  subgraph cluster_aop {
    style=invis;
    "0" [label="<AutoLogAspect: void before(JoinPoint)>", fillcolor="lightyellow"];
    "7" [label="<DictAspect: Object around(ProceedingJoinPoint)>", fillcolor="lightyellow"];
  }

  { rank=same; "1"; }
  "1" [label="<SysUserController: Result queryPageList(SysUser,Integer,Integer,HttpServletRequest)>"];

  { rank=same; "2"; }
  "2" [label="<SysUserServiceImpl: IPage queryPageList(SysUser,Page)>"];

  { rank=same; "3"; "4"; }
  "3" [label="<SysUserMapper: IPage selectPage(Page,QueryWrapper)>"];
  "4" [label="<QueryGenerator: QueryWrapper initQueryWrapper(Object,Map)>"];

  { rank=same; "5"; "6"; }
  "5" [label="<ServiceImpl: IPage page(IPage,Wrapper)>"];
  "6" [label="<BaseMapper: IPage selectPage(IPage,Wrapper)>"];

  "1" -> "2" [style=invis, weight=100];
  "2" -> "3" [style=invis, weight=100];

  "1" -> "0" [label="[AOP-BEFORE]", color=red, style=dashed, fontcolor=red, penwidth=2, constraint=false];
  "1" -> "7" [label="[AOP-AROUND]", color=red, style=dashed, fontcolor=red, penwidth=2, constraint=false];
  "1" -> "2" [label="queryPageList()", color="black", weight=10];
  "1" -> "4" [label="initQueryWrapper()", color="black", weight=10];
  "2" -> "3" [label="selectPage()", color="black", weight=10];
  "2" -> "5" [label="page()", color="black", weight=10];
  "5" -> "6" [label="selectPage()", color="black", weight=10];
  "7" -> "1" [label="proceed()", color="#FF8C00", fontcolor="#FF8C00", penwidth=1.5, weight=5];
}`,

  'SysUserController.add': `digraph G {
  rankdir=TB;
  ranksep=1.0;
  nodesep=0.5;
  node [shape=box, style=filled, fillcolor="lightblue"];
  edge [color="black"];

  subgraph cluster_legend {
    label="Legend";
    style=dashed;
    fontsize=10;
    rank=source;
    node [shape=plaintext];
    legend [label=<
      <TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">
        <TR><TD BGCOLOR="lightblue"><B>Business Logic</B></TD><TD>Black Solid Line</TD></TR>
        <TR><TD BGCOLOR="lightyellow"><B>AOP Advice</B></TD><TD><FONT COLOR="red">Red Dashed Line (Weaving)</FONT></TD></TR>
      </TABLE>
    >];
  }

  subgraph cluster_aop {
    style=invis;
    "0" [label="<AutoLogAspect: void before(JoinPoint)>", fillcolor="lightyellow"];
  }

  { rank=same; "1"; }
  "1" [label="<SysUserController: Result add(JSONObject)>"];

  { rank=same; "2"; }
  "2" [label="<SysUserServiceImpl: void addUserWithRole(JSONObject)>"];

  { rank=same; "3"; "4"; }
  "3" [label="<SysUserMapper: int insert(SysUser)>"];
  "4" [label="<SysUserRoleMapper: int insert(SysUserRole)>"];

  "1" -> "2" [style=invis, weight=100];
  "2" -> "3" [style=invis, weight=100];

  "1" -> "0" [label="[AOP-BEFORE]", color=red, style=dashed, fontcolor=red, penwidth=2, constraint=false];
  "1" -> "2" [label="addUserWithRole()", color="black", weight=10];
  "2" -> "3" [label="insert()", color="black", weight=10];
  "2" -> "4" [label="insert()", color="black", weight=10];
}`,

  'SysUserController.edit': `digraph G {
  rankdir=TB;
  ranksep=1.0;
  nodesep=0.5;
  node [shape=box, style=filled, fillcolor="lightblue"];
  edge [color="black"];

  { rank=same; "1"; }
  "1" [label="<SysUserController: Result edit(JSONObject)>"];

  { rank=same; "2"; }
  "2" [label="<SysUserServiceImpl: void editUserWithRole(JSONObject)>"];

  { rank=same; "3"; "4"; }
  "3" [label="<SysUserMapper: int updateById(SysUser)>"];
  "4" [label="<SysUserRoleMapper: int delete(QueryWrapper)>"];

  "1" -> "2" [style=invis, weight=100];
  "2" -> "3" [style=invis, weight=100];

  "1" -> "2" [label="editUserWithRole()", color="black", weight=10];
  "2" -> "3" [label="updateById()", color="black", weight=10];
  "2" -> "4" [label="delete()", color="black", weight=10];
}`,

  'SysUserController.delete': `digraph G {
  rankdir=TB;
  ranksep=1.0;
  nodesep=0.5;
  node [shape=box, style=filled, fillcolor="lightblue"];
  edge [color="black"];

  { rank=same; "1"; }
  "1" [label="<SysUserController: Result delete(String)>"];

  { rank=same; "2"; }
  "2" [label="<SysUserServiceImpl: boolean removeById(String)>"];

  { rank=same; "3"; }
  "3" [label="<SysUserMapper: int deleteById(String)>"];

  "1" -> "2" [style=invis, weight=100];
  "2" -> "3" [style=invis, weight=100];

  "1" -> "2" [label="removeById()", color="black", weight=10];
  "2" -> "3" [label="deleteById()", color="black", weight=10];
}`,

  'LoginController.login': `digraph G {
  rankdir=TB;
  ranksep=1.0;
  nodesep=0.5;
  node [shape=box, style=filled, fillcolor="lightblue"];
  edge [color="black"];

  subgraph cluster_aop {
    style=invis;
    "0" [label="<AutoLogAspect: void before(JoinPoint)>", fillcolor="lightyellow"];
  }

  { rank=same; "1"; }
  "1" [label="<LoginController: Result login(SysLoginModel)>"];

  { rank=same; "2"; "3"; }
  "2" [label="<SysUserServiceImpl: SysUser getUserByName(String)>"];
  "3" [label="<JwtUtil: String sign(String,String)>"];

  { rank=same; "4"; }
  "4" [label="<SysUserMapper: SysUser getUserByName(String)>"];

  "1" -> "2" [style=invis, weight=100];
  "2" -> "4" [style=invis, weight=100];

  "1" -> "0" [label="[AOP-BEFORE]", color=red, style=dashed, fontcolor=red, penwidth=2, constraint=false];
  "1" -> "2" [label="getUserByName()", color="black", weight=10];
  "1" -> "3" [label="sign()", color="black", weight=10];
  "2" -> "4" [label="getUserByName()", color="black", weight=10];
}`
}

// Taint analysis source config
export const taintSources = [
  { kind: 'param', method: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result queryPageList(org.jeecg.modules.system.entity.SysUser,java.lang.Integer,java.lang.Integer,javax.servlet.http.HttpServletRequest)>', index: 1, type: 'java.lang.Integer' },
  { kind: 'param', method: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result queryPageList(org.jeecg.modules.system.entity.SysUser,java.lang.Integer,java.lang.Integer,javax.servlet.http.HttpServletRequest)>', index: 2, type: 'java.lang.Integer' },
  { kind: 'param', method: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result add(com.alibaba.fastjson.JSONObject)>', index: 0, type: 'com.alibaba.fastjson.JSONObject' },
  { kind: 'param', method: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result edit(com.alibaba.fastjson.JSONObject)>', index: 0, type: 'com.alibaba.fastjson.JSONObject' },
  { kind: 'param', method: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result delete(java.lang.String)>', index: 0, type: 'java.lang.String' },
  { kind: 'param', method: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result deleteBatch(java.lang.String)>', index: 0, type: 'java.lang.String' },
  { kind: 'param', method: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result queryById(java.lang.String)>', index: 0, type: 'java.lang.String' },
  { kind: 'param', method: '<org.jeecg.modules.system.controller.SysUserController: org.jeecg.common.api.vo.Result changePassword(org.jeecg.modules.system.entity.SysUser)>', index: 0, type: 'org.jeecg.modules.system.entity.SysUser' },
  { kind: 'param', method: '<org.jeecg.modules.system.controller.LoginController: org.jeecg.common.api.vo.Result login(org.jeecg.modules.system.model.SysLoginModel)>', index: 0, type: 'org.jeecg.modules.system.model.SysLoginModel' },
  { kind: 'param', method: '<org.jeecg.modules.system.controller.LoginController: org.jeecg.common.api.vo.Result checkCaptcha(java.lang.String,java.lang.String)>', index: 0, type: 'java.lang.String' },
  { kind: 'param', method: '<org.jeecg.modules.system.controller.LoginController: org.jeecg.common.api.vo.Result checkCaptcha(java.lang.String,java.lang.String)>', index: 1, type: 'java.lang.String' },
  { kind: 'param', method: '<org.jeecg.modules.system.controller.SysDictController: org.jeecg.common.api.vo.Result queryPageList(org.jeecg.modules.system.entity.SysDict,java.lang.Integer,java.lang.Integer,javax.servlet.http.HttpServletRequest)>', index: 1, type: 'java.lang.Integer' },
  { kind: 'param', method: '<org.jeecg.modules.system.controller.SysDictController: org.jeecg.common.api.vo.Result queryPageList(org.jeecg.modules.system.entity.SysDict,java.lang.Integer,java.lang.Integer,javax.servlet.http.HttpServletRequest)>', index: 2, type: 'java.lang.Integer' },
  { kind: 'param', method: '<org.jeecg.modules.demo.test.controller.JeecgDemoController: org.jeecg.common.api.vo.Result list(org.jeecg.modules.demo.test.entity.JeecgDemo,java.lang.Integer,java.lang.Integer,javax.servlet.http.HttpServletRequest)>', index: 1, type: 'java.lang.Integer' },
  { kind: 'param', method: '<org.jeecg.modules.demo.test.controller.JeecgDemoController: org.jeecg.common.api.vo.Result list(org.jeecg.modules.demo.test.entity.JeecgDemo,java.lang.Integer,java.lang.Integer,javax.servlet.http.HttpServletRequest)>', index: 2, type: 'java.lang.Integer' },
  { kind: 'param', method: '<org.jeecg.modules.demo.test.controller.JeecgDemoController: org.jeecg.common.api.vo.Result add(org.jeecg.modules.demo.test.entity.JeecgDemo)>', index: 0, type: 'org.jeecg.modules.demo.test.entity.JeecgDemo' },
  { kind: 'param', method: '<org.jeecg.modules.demo.test.controller.JeecgDemoController: org.jeecg.common.api.vo.Result delete(java.lang.String)>', index: 0, type: 'java.lang.String' }
]

// Taint analysis sink config
export const taintSinks = [
  { method: '<org.jeecg.modules.system.mapper.SysUserMapper: java.util.List getUserByDepId(java.lang.String)>', index: 0, sql: 'SELECT * FROM sys_user WHERE dep_id = ${depId}', risk: 'SQL Injection' },
  { method: '<org.jeecg.modules.system.mapper.SysUserMapper: java.util.List getUserByRealname(java.lang.String)>', index: 0, sql: "SELECT * FROM sys_user WHERE realname LIKE '%${realname}%'", risk: 'SQL Injection' },
  { method: '<org.jeecg.modules.system.mapper.SysUserMapper: java.util.List queryByDepartIds(java.lang.String)>', index: 0, sql: 'SELECT * FROM sys_user WHERE depart_ids IN (${departIds})', risk: 'SQL Injection' },
  { method: '<org.jeecg.modules.system.mapper.SysDictMapper: java.util.List queryTableDictByCode(java.lang.String,java.lang.String,java.lang.String)>', index: 0, sql: 'SELECT ${field} AS "value", ${text} AS "text" FROM ${table}', risk: 'SQL Injection' },
  { method: '<org.jeecg.modules.system.mapper.SysDictMapper: java.util.List queryTableDictByCode(java.lang.String,java.lang.String,java.lang.String)>', index: 1, sql: 'SELECT ${field} AS "value", ${text} AS "text" FROM ${table}', risk: 'SQL Injection' },
  { method: '<org.jeecg.modules.system.mapper.SysDictMapper: java.util.List queryTableDictByCode(java.lang.String,java.lang.String,java.lang.String)>', index: 2, sql: 'SELECT ${field} AS "value", ${text} AS "text" FROM ${table}', risk: 'SQL Injection' },
  { method: '<org.jeecg.modules.system.mapper.SysDictMapper: java.lang.String queryDictTextByKey(java.lang.String,java.lang.String,java.lang.String)>', index: 0, sql: "SELECT ${column} FROM ${table} WHERE ${key} = #{value}", risk: 'SQL Injection' },
  { method: '<org.jeecg.modules.system.mapper.SysDictMapper: java.lang.String queryDictTextByKey(java.lang.String,java.lang.String,java.lang.String)>', index: 1, sql: "SELECT ${column} FROM ${table} WHERE ${key} = #{value}", risk: 'SQL Injection' }
]

// Taint flow paths (simulating taint-flow-graph.dot)
export const taintFlows = [
  {
    id: 1,
    type: 'SQL Injection',
    severity: 'HIGH',
    source: { method: 'SysDictController.queryPageList()', param: 'HttpServletRequest', index: 3 },
    sink: { method: 'SysDictMapper.queryTableDictByCode()', param: 'table', index: 0, sql: 'SELECT ${field} FROM ${table}' },
    path: [
      'SysDictController.queryPageList(SysDict, Integer, Integer, HttpServletRequest)',
      'SysDictServiceImpl.queryTableDictItemsByCode(String, String, String)',
      'SysDictMapper.queryTableDictByCode(String, String, String)'
    ]
  },
  {
    id: 2,
    type: 'SQL Injection',
    severity: 'HIGH',
    source: { method: 'SysUserController.queryPageList()', param: 'SysUser', index: 0 },
    sink: { method: 'SysUserMapper.getUserByRealname()', param: 'realname', index: 0, sql: "SELECT * FROM sys_user WHERE realname LIKE '%${realname}%'" },
    path: [
      'SysUserController.queryPageList(SysUser, Integer, Integer, HttpServletRequest)',
      'SysUserServiceImpl.queryUserList(SysUser, Page)',
      'SysUserMapper.getUserByRealname(String)'
    ]
  },
  {
    id: 3,
    type: 'SQL Injection',
    severity: 'HIGH',
    source: { method: 'SysUserController.queryPageList()', param: 'SysUser', index: 0 },
    sink: { method: 'SysUserMapper.getUserByDepId()', param: 'depId', index: 0, sql: 'SELECT * FROM sys_user WHERE dep_id = ${depId}' },
    path: [
      'SysUserController.queryPageList(SysUser, Integer, Integer, HttpServletRequest)',
      'SysUserServiceImpl.queryUserList(SysUser, Page)',
      'SysUserMapper.getUserByDepId(String)'
    ]
  },
  {
    id: 4,
    type: 'SQL Injection',
    severity: 'MEDIUM',
    source: { method: 'SysUserController.delete()', param: 'id (String)', index: 0 },
    sink: { method: 'SysUserMapper.queryByDepartIds()', param: 'departIds', index: 0, sql: 'SELECT * FROM sys_user WHERE depart_ids IN (${departIds})' },
    path: [
      'SysUserController.delete(String)',
      'SysUserServiceImpl.deleteUser(String)',
      'SysUserServiceImpl.checkDepart(String)',
      'SysUserMapper.queryByDepartIds(String)'
    ]
  },
  {
    id: 5,
    type: 'SQL Injection',
    severity: 'HIGH',
    source: { method: 'SysDictController.queryAllDictItems()', param: 'N/A', index: -1 },
    sink: { method: 'SysDictMapper.queryDictTextByKey()', param: 'table', index: 0, sql: "SELECT ${column} FROM ${table} WHERE ${key} = #{value}" },
    path: [
      'SysDictController.queryAllDictItems()',
      'SysDictServiceImpl.queryAllDictItems()',
      'SysDictMapper.queryDictTextByKey(String, String, String)'
    ]
  }
]

// Taint flow DOT graph
export const taintFlowDot = `digraph TaintFlowGraph {
  rankdir=LR;
  node [shape=box, style=filled];
  edge [color="#666"];

  subgraph cluster_legend {
    label="Taint Flow Legend";
    style=dashed;
    node [shape=plaintext];
    legend [label=<
      <TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">
        <TR><TD BGCOLOR="#ffcccc"><B>Source (User Input)</B></TD></TR>
        <TR><TD BGCOLOR="#ffffcc"><B>Transfer (Propagation)</B></TD></TR>
        <TR><TD BGCOLOR="#ccffcc"><B>Sink (Dangerous Operation)</B></TD></TR>
      </TABLE>
    >];
  }

  // Flow 1
  "src1" [label="SysDictController\\n.queryPageList()\\n[HttpServletRequest]", fillcolor="#ffcccc"];
  "t1" [label="SysDictServiceImpl\\n.queryTableDictItemsByCode()", fillcolor="#ffffcc"];
  "sink1" [label="SysDictMapper\\n.queryTableDictByCode()\\n[SQL: SELECT \\$\\{field\\}]", fillcolor="#ccffcc"];
  "src1" -> "t1" -> "sink1";

  // Flow 2
  "src2" [label="SysUserController\\n.queryPageList()\\n[SysUser.realname]", fillcolor="#ffcccc"];
  "t2" [label="SysUserServiceImpl\\n.queryUserList()", fillcolor="#ffffcc"];
  "sink2" [label="SysUserMapper\\n.getUserByRealname()\\n[SQL: LIKE %\\$\\{realname\\}%]", fillcolor="#ccffcc"];
  "src2" -> "t2" -> "sink2";

  // Flow 3
  "src3" [label="SysUserController\\n.queryPageList()\\n[SysUser.depId]", fillcolor="#ffcccc"];
  "t3" [label="SysUserServiceImpl\\n.queryUserList()", fillcolor="#ffffcc"];
  "sink3" [label="SysUserMapper\\n.getUserByDepId()\\n[SQL: dep_id = \\$\\{depId\\}]", fillcolor="#ccffcc"];
  "src3" -> "t3" -> "sink3";
}`
