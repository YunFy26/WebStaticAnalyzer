# 图3-1 基于静态规则与应用程序上下文的Spring Web应用静态分析方法整体架构

```mermaid
flowchart TB
    subgraph Input["输入"]
        A[Spring Web应用源代码]
    end
    
    subgraph Phase1["预处理阶段"]
        B[源代码转换为中间代码]
        C[注解识别与收集]
        D[注解元数据转换为静态规则]
        E[提取IoC容器管理的对象类型]
        F[提取Web请求入口点]
        G[入口参数建模]
        
        B --> C
        C --> D
        D --> E
        D --> F
        F --> G
    end
    
    subgraph Phase2["依赖注入处理阶段"]
        H["识别注入字段(@Autowired)"]
        I[获取注入字段相关的方法调用语句]
        J[推导实际注入类型]
        K[更新变量指针集建立对象关系]
        
        H --> I
        I --> J
        J --> K
    end
    
    subgraph Phase3["面向切面编程处理阶段"]
        L["识别切面类(@Aspect)"]
        M["提取切入点表达式(@Pointcut)"]
        N["提取通知方法(@Before/@After/@Around)"]
        O[匹配目标方法集合]
        P[静态构建增强后的方法调用序列]
        
        L --> M
        L --> N
        M --> O
        N --> O
        O --> P
    end
    
    subgraph Core["Tai-e静态分析框架"]
        Q[指针分析]
        R[调用图构建]
        S[控制流分析]
        T[数据流分析]
    end
    
    subgraph Output["输出"]
        U[完整的调用图]
        V[准确的指针集]
        W[完整的控制流和数据流]
    end
    
    A --> B
    G --> H
    K --> L
    P --> Q
    
    Q --> R
    R --> S
    S --> T
    
    T --> U
    T --> V
    T --> W
    
    style Phase1 fill:#E3F2FD
    style Phase2 fill:#F3E5F5
    style Phase3 fill:#FFF3E0
    style Core fill:#E8F5E9
    style Input fill:#FAFAFA
    style Output fill:#FAFAFA
```

## 图3-1说明

本架构图展示了Spring Web应用静态分析方法的三阶段处理流程：

### 预处理阶段
- **目标**：解决Web应用多入口点问题
- **主要任务**：
    - 将源代码转换为中间代码表示
    - 识别并收集Spring相关注解（@Controller、@Service、@RequestMapping等）
    - 将注解元数据转换为静态分析规则
    - 提取IoC容器管理的所有Bean类型
    - 提取所有Web请求入口点并进行参数建模

### 依赖注入处理阶段
- **目标**：解决依赖注入导致的对象关系不明确问题
- **主要任务**：
    - 识别带有@Autowired等注解的注入字段
    - 获取所有与注入字段相关的方法调用语句
    - 根据字段声明类型推导实际注入的具体实现类型
    - 将推导出的实际类型对象映射到变量指针集中

### 面向切面编程处理阶段
- **目标**：解决AOP动态代理导致的方法调用缺失问题
- **主要任务**：
    - 识别系统中的切面类（@Aspect）
    - 提取切入点表达式（@Pointcut）和通知方法（@Before、@After、@Around等）
    - 根据切入点表达式匹配受影响的目标方法集合
    - 静态构建包含通知方法的完整调用序列，模拟代理对象行为

### Tai-e静态分析框架
基于前三阶段的处理结果，执行核心静态分析：
- 指针分析
- 调用图构建
- 控制流分析
- 数据流分析

### 输出结果
生成准确完整的分析结果：
- 完整的调用图
- 准确的指针集
- 完整的控制流和数据流信息