# easy-event
基于JSR269实现的事件驱动组件
image.png![image](https://user-images.githubusercontent.com/79463662/116644930-3d6b3200-a9a7-11eb-9c23-74d4a2dfb2fb.png)
---
###使用方法
#### 1.实现EventConsumer接口, 如下
```java
public class MyConsumer implements EventConsumer {

    @Override
    public void before(AroundMethod aroundMethod) {
        System.out.println("consume before");
    }

    @Override
    public void after(AroundMethod aroundMethod) {
        System.out.println("consume after");
    }

    @Override
    public void thrown(AroundMethod aroundMethod, Exception e) {
        System.out.println("consume thrown");
    }
    
    @Override
    public Executor executor() {
        return new ThreadPoolExecutor(4, 8, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(100));
    }
}
```

#### 2.目标方法上添加 @Event 注解
```java
@Event({MyConsumer.class})
private int method1(String param) {
    System.out.println("业务逻辑:param =" + param);
    return 1;
}
```

如果是Spring项目，则在spring xml配置中添加如下配置，并将 MyConsumer 注入到Spring容器中
```xml
<bean class="com.github.binarybeing.easyevent.ConsumersConfiguration"/>
```
