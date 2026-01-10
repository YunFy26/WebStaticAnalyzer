org.example.spring
- `BeanAnalysis`: 

- `BeanAnnotationRules`

- `BeanInfo`

- `ControllerClass`

- `DICGConstructorPlugin`

- `InjectionAnnotationRules`

- `InjectPoint`

- `RouterAnalysis`

- `RouterMethod`

./mvnw dependency:copy-dependencies -DoutputDirectory=target/dependency

用户需要将SpringTarget项目中的依赖库添加到Tai-e的分析配置中，特别是要包含jakarta.servlet.http.HttpServletRequestWrapper类。
从之前的依赖树中，我们知道tomcat-embed-core-10.1.44.jar包含了所需的类。这个jar包位于Maven本地仓库中。另外，为了确保所有依赖都被包含，我们可以将Maven本地仓库的路径添加到Tai-e的配置中，或者只添加必要的jar包。
但是，由于项目使用了Maven，我们可以通过Maven命令将所有的依赖复制到一个目录中，然后在配置中指定这个目录。
步骤：
在项目根目录下执行：./mvnw dependency:copy-dependencies -DoutputDirectory=target/dependency
这将把所有的依赖（包括传递依赖）复制到target/dependency目录。
然后在Tai-e配置文件中，将classPath设置为这个目录下的所有jar包。