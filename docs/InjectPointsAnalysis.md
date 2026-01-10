依赖注入的Field声明方式
```java
public interface UserService {
    public String getUser();
    public String getId();
}

@Service
public class UserServiceImpl implements UserService {
    @Override
    public String getUser() {
        return "User from UserServiceImpl";
    }
    @Override
    public String getId() {
        return "ID from UserServiceImpl";
    }
}

public abstract class UserServiceAbstract {

    public String getUserInfo() {
        return "User: " + getUser() + ", ID: " + getId();
    }

    protected abstract String getUser();
    protected abstract String getId();
}

@Service
public class UserServiceAbstractImpl extends UserServiceAbstract{
    @Override
    public String getUser() {
        return "";
    }

    @Override
    public String getId() {
        return "";
    }
}

@Controller
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private UserServiceAbstract userServiceAbstract;
    @Autowired
    private UserServiceImpl userServiceImpl;

    public void getUser() {
        userService.getUser();
        userServiceAbstract.getUser();
        userServiceImpl.getUser();
    }

    public void getUserAndId() {
        userService.getUser();
        userService.getId();
    }
}
```
对于Spring依赖注入，Field声明时，存在三种方式，一种是接口类型，一种是抽象类类型，一种是实现类类型。
在项目中常用接口类型进行依赖注入，方便解耦。

UserController有三个Field，这三个Field的类型一个是接口UserService，一个是抽象类UserServiceAbstract，一个是实现类UserServiceImpl，。
在getUser方法中，分别调用了这三种类型的getUser方法
```java
public void getUser() {
        userService.getUser();
        userServiceAbstract.getUser();
        userServiceImpl.getUser();
    }
```
其IR为
```java
public void getUser() {
        org.example.springtarget.example.UserService $r1;
        org.example.springtarget.example.UserServiceAbstract $r2;
        org.example.springtarget.example.UserServiceImpl $r3;
        [0@L16] $r1 = %this.<org.example.springtarget.example.UserController: org.example.springtarget.example.UserService userService>;
        [1@L16] invokeinterface $r1.<org.example.springtarget.example.UserService: java.lang.String getUser()>();
        [2@L17] $r2 = %this.<org.example.springtarget.example.UserController: org.example.springtarget.example.UserServiceAbstract userServiceAbstract>;
        [3@L17] invokevirtual $r2.<org.example.springtarget.example.UserServiceAbstract: java.lang.String getUser()>();
        [4@L18] $r3 = %this.<org.example.springtarget.example.UserController: org.example.springtarget.example.UserServiceImpl userServiceImpl>;
        [5@L18] invokevirtual $r3.<org.example.springtarget.example.UserServiceImpl: java.lang.String getUser()>();
        [6@L19] return;
    }
```
可以看到，调用接口类型的Field时，使用invokeinterface指令，调用抽象类类型和实现类类型的Field时，使用invokevirtual指令。
这和第xx节理论分析是一致的。

第xx行
```java
        [0@L16] $r1 = %this.<org.example.springtarget.example.UserController: org.example.springtarget.example.UserService userService>;

```
是LoadField语句，表示把类UserController的Field userService加载到局部变量$r1中
然后通过局部变量$r1进行方法调用
```java
        [5@L18] invokevirtual $r3.<org.example.springtarget.example.UserServiceImpl: java.lang.String getUser()>();
```

对于每个Bean，Spring容器会根据其类型进行实例化和注入。
所以，在分析时，分析该Bean的每个method
在这个method中，分析每个LoadField语句，将Field与局部变量关联起来
然后分析每个方法调用语句，找到调用的局部变量
通过局部变量找到对应的Field，根据该Field的类型在bean集合中找对应的Bean实例
如果该Field的类型是接口类型，找到bean集合中实现该接口的Bean实例
    如果有多个Bean实现了该接口，则需要结合其他信息（如@Qualifier注解）来确定具体的Bean实例
如果该Field的类型是抽象类类型，找到bean集合中继承该抽象类的Bean实例
    如果有多个Bean继承了该抽象类，则需要结合其他信息（如@Qualifier注解）来确定具体的Bean实例
如果该Field的类型是实现类类型，直接找到该实现类类型的Bean实例
这样就能准确地找到依赖注入的Bean实例

```java
@org.springframework.stereotype.Controller
public class org.example.springtarget.example.UserController extends java.lang.Object {

    @org.springframework.beans.factory.annotation.Autowired
    private org.example.springtarget.example.UserService userService;

    @org.springframework.beans.factory.annotation.Autowired
    private org.example.springtarget.example.UserServiceAbstract userServiceAbstract;

    @org.springframework.beans.factory.annotation.Autowired
    private org.example.springtarget.example.UserServiceImpl userServiceImpl;

    public void <init>() {
        [0@L7] invokespecial %this.<java.lang.Object: void <init>()>();
        [1@L7] return;
    }

    public void getUser() {
        org.example.springtarget.example.UserService $r1;
        org.example.springtarget.example.UserServiceAbstract $r2;
        org.example.springtarget.example.UserServiceImpl $r3;
        [0@L16] $r1 = %this.<org.example.springtarget.example.UserController: org.example.springtarget.example.UserService userService>;
        [1@L16] invokeinterface $r1.<org.example.springtarget.example.UserService: java.lang.String getUser()>();
        [2@L17] $r2 = %this.<org.example.springtarget.example.UserController: org.example.springtarget.example.UserServiceAbstract userServiceAbstract>;
        [3@L17] invokevirtual $r2.<org.example.springtarget.example.UserServiceAbstract: java.lang.String getUser()>();
        [4@L18] $r3 = %this.<org.example.springtarget.example.UserController: org.example.springtarget.example.UserServiceImpl userServiceImpl>;
        [5@L18] invokevirtual $r3.<org.example.springtarget.example.UserServiceImpl: java.lang.String getUser()>();
        [6@L19] return;
    }

    public void getUserAndId() {
        org.example.springtarget.example.UserService $r1, $r2;
        [0@L22] $r1 = %this.<org.example.springtarget.example.UserController: org.example.springtarget.example.UserService userService>;
        [1@L22] invokeinterface $r1.<org.example.springtarget.example.UserService: java.lang.String getUser()>();
        [2@L23] $r2 = %this.<org.example.springtarget.example.UserController: org.example.springtarget.example.UserService userService>;
        [3@L23] invokeinterface $r2.<org.example.springtarget.example.UserService: java.lang.String getId()>();
        [4@L24] return;
    }

}
```