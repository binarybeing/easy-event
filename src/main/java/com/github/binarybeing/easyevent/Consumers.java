package com.github.binarybeing.easyevent;

import com.github.binarybeing.easyevent.model.AroundMethod;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Author binarybeing
 * @Date 2021-02-09
 **/
public final class Consumers {

    private static Map<Class<? extends EventConsumer>, EventConsumerProxy> enhancerMap = new ConcurrentHashMap<>();
    private static ApplicationContext springContext;
    private static Logger errorLogger = Logger.getLogger(Consumers.class);


    public static EventConsumer getConsumer(Class<? extends EventConsumer> clazz) {
        return enhancerMap.computeIfAbsent(clazz, aClass -> {
            EventConsumer enhancer = null;
            try {
                enhancer = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                errorLogger.warn("clazz.newInstance error " + clazz.getCanonicalName(), e);
            }
            return new EventConsumerProxy(enhancer) ;
        });
    }

    public static void invokeBefore(List<EventConsumer> consumers, AroundMethod aroundMethod) {
        if (CollectionUtils.isEmpty(consumers)) {
            return;
        }
        for (EventConsumer consumer : consumers) {
            try {
                consumer.before(aroundMethod);
            } catch (Exception e) {
                errorLogger.error(
                        "consume before fail, consumer class = " + consumer.getClass() + " target class=" + aroundMethod.getMethodInfo().getDeclaringClass()
                                + " target method= " + aroundMethod.getMethodInfo().getName(), e);
            }
        }
    }

    public static void invokeAfter(List<EventConsumer> consumers, AroundMethod aroundMethod) {
        if (CollectionUtils.isEmpty(consumers)) {
            return;
        }
        for (EventConsumer consumer : consumers) {
            try {
                consumer.after(aroundMethod);
            } catch (Exception e) {
                errorLogger.error(
                        "consume after fail, consumer class = " + consumer.getClass() + " target class=" + aroundMethod.getMethodInfo().getDeclaringClass()
                                + " target method= " + aroundMethod.getMethodInfo().getName(), e);
            }
        }
    }

    public static void invokeThrown(List<EventConsumer> consumers, AroundMethod aroundMethod, Exception exp) {
        if (CollectionUtils.isEmpty(consumers)) {
            return;
        }
        for (EventConsumer consumer : consumers) {
            try {
                consumer.thrown(aroundMethod, exp);
            } catch (Exception e) {
                errorLogger.error(
                        "consume thrown fail, consumer class = " + consumer.getClass() + " target class=" + aroundMethod.getMethodInfo().getDeclaringClass()
                                + " target method= " + aroundMethod.getMethodInfo().getName(), e);
            }
        }
    }

    static void setApplicationContext(ApplicationContext applicationContext) {
        springContext = applicationContext;
        Set<Class<? extends EventConsumer>> set = enhancerMap.keySet();
        for (Class<? extends EventConsumer> clazz : set) {
            EventConsumerProxy proxy = enhancerMap.get(clazz);
            EventConsumer consumer = springContext.getBean(clazz);
            proxy.setConsumer(consumer);
            enhancerMap.put(clazz, proxy);
        }
    }

    private static class EventConsumerProxy implements EventConsumer{

        private EventConsumer consumer;

        EventConsumerProxy(EventConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public void before(AroundMethod targetMethod) {
            if (null == consumer) {
                return;
            }
            consumer.before(targetMethod);
        }

        @Override
        public void after(AroundMethod targetMethod) {
            if (null == consumer) {
                return;
            }
            consumer.after(targetMethod);
        }

        @Override
        public void thrown(AroundMethod targetMethod, Exception e) {
            if (null == consumer) {
                return;
            }
            consumer.thrown(targetMethod, e);
        }

        void setConsumer(EventConsumer consumer) {
            this.consumer = consumer;
        }
    }
}
