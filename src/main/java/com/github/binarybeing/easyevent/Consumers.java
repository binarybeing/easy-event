package com.github.binarybeing.easyevent;

import com.github.binarybeing.easyevent.model.AroundMethod;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * @Description
 * @Author binarybeing
 * @Date 2021-02-09
 **/
public final class Consumers {

    private static Map<Class<? extends EventConsumer>, EventConsumerWrapper> enhancerMap = new ConcurrentHashMap<>();
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
            return new EventConsumerWrapper(enhancer) ;
        });
    }

    public static void invokeBefore(List<EventConsumer> consumers, AroundMethod aroundMethod) {
        if (CollectionUtils.isEmpty(consumers)) {
            return;
        }
        for (EventConsumer consumer : consumers) {
            try {
                Runnable runnable = () -> consumer.before(aroundMethod);
                execute(consumer, runnable);
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
                Runnable runnable = () -> consumer.after(aroundMethod);
                execute(consumer, runnable);
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
                Runnable runnable = () -> consumer.thrown(aroundMethod, exp);
                execute(consumer, runnable);
            } catch (Exception e) {
                errorLogger.error(
                        "consume thrown fail, consumer class = " + consumer.getClass() + " target class=" + aroundMethod.getMethodInfo().getDeclaringClass()
                                + " target method= " + aroundMethod.getMethodInfo().getName(), e);
            }
        }
    }

    private static void execute(EventConsumer consumer, Runnable runnable) {
        if (consumer != null && consumer.executor() != null) {
            consumer.executor().execute(runnable);
        }
    }

    static void setApplicationContext(ApplicationContext applicationContext) {
        springContext = applicationContext;
        Set<Class<? extends EventConsumer>> set = enhancerMap.keySet();
        for (Class<? extends EventConsumer> clazz : set) {
            EventConsumerWrapper proxy = enhancerMap.get(clazz);
            EventConsumer consumer = springContext.getBean(clazz);
            proxy.setConsumer(consumer);
            enhancerMap.put(clazz, proxy);
        }
    }

    private static class EventConsumerWrapper implements EventConsumer{

        private EventConsumer consumer;

        EventConsumerWrapper(EventConsumer consumer) {
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

        @Override
        public Executor executor() {
            if (null == consumer) {
                return null;
            }
            return consumer.executor();
        }

        void setConsumer(EventConsumer consumer) {
            this.consumer = consumer;
        }
    }
}
