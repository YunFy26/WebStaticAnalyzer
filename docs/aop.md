1. execution：精确制导
   execution是功能最强大、使用最广泛的切点指示符。它允许你通过方法签名来精确匹配连接点。
   ​​语法结构​​：execution(修饰符? 返回类型 类路径?方法名(参数列表) 异常类型?)，其中带 ?的部分是可选的
   。
   ​​通配符​​：
   *：匹配任意数量的字符（但只能匹配一级包或一个类名）。
   ..：匹配任意数量的字符（可用于包路径表示多级子包）或匹配任意个数、任意类型的参数
   。

within：批量拦截
within的匹配粒度较粗，它不关心具体的方法签名，只关注方法所在的类型（类或包）。

匹配​​带有指定注解的方法​​。只要方法上标注了该注解，就会被拦截
根据​​自定义注解​​来触发特定逻辑，如权限检查、日志记录等，非常灵活
。


```java
Algorithm: processDI
Input: applicationClasses: Application classes of project analyzed

```

```java
Algorithm: processAOP
Input: applicationClasses: Application classes of project analyzed
Output: aspectClasses: List of aspect classes with their pointcuts and advice methods
        weavingMap: Map<targetMethod, List<aspectWeaving>> mapping target methods to their aspects

1  aspectClasses ← []
2  namedPointcuts ← {}
3  pointcutCache ← {}
4  
5  // 步骤1: 扫描所有切面类
6  for each class in applicationClasses:
7      if class match AspectRules.ASPECT:
8    
11         for each method in class:
12             if method match AspectRules.POINTCUT || AspectRules.ADVICE:
                   获取表达式
13                 expression ← method.annotation.value
14                 pointcut ← new Pointcut(method)
15                 pointcut.expression ← expression
16                 pointcut.type ← parsePointcutType(expression)
17                 namedPointcuts[method.name] ← pointcut
18         
19         // 步骤3: 解析通知方法
20         for each method in class.declaredMethods:
21             adviceType ← matchAdviceAnnotation(method)
22             if adviceType is not null:
23                 aspectMethod ← new AspectMethod(method)
24                 aspectMethod.adviceType ← adviceType
25                 
26                 expression ← getPointcutExpression(method)
27                 pointcut ← resolvePointcut(expression, namedPointcuts, pointcutCache)
28                 
29                 if pointcut is not null:
30                     aspectMethod.pointcut ← pointcut
31                     aspectClass.addAspectMethod(pointcut, aspectMethod)
32         
33         add aspectClass to aspectClasses
34  
35  return aspectClasses

// 辅助函数: 解析切点
function resolvePointcut(expression, namedPointcuts, pointcutCache):
36     // 命名切点引用
37     if expression matches "methodName()":
38         return namedPointcuts[methodName]
39     
40     // 组合切点
41     if expression contains logical operators (&&, ||, !, and, or, not):
42         pointcut ← new Pointcut(null)
43         pointcut.type ← COMBINED
44         return pointcutCache.computeIfAbsent(pointcut)
45     
46     // 内联切点
47     pointcut ← new Pointcut(null)
48     pointcut.expression ← expression
49     pointcut.type ← parsePointcutType(expression)
50     return pointcutCache.computeIfAbsent(pointcut)

// 辅助函数: 解析切点类型
function parsePointcutType(expression):
51     if expression contains logical operators:
52         return COMBINED
53     for each prefix in POINTCUT_PREFIX_MAP:
54         if expression startsWith prefix:
55             return POINTCUT_PREFIX_MAP[prefix]
56     return EXECUTION

```
// 切面相关规则
AspectRules.Advice
AspectRules.Aspect
