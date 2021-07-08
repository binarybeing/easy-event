package com.github.binarybeing.easyevent;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * @Description
 * @Author junhua5
 * @Date 2021-03-06
 **/
public class ConsumersConfiguration implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, EventConsumer> beansOfType = applicationContext.getBeansOfType(EventConsumer.class);
        for (EventConsumer value : beansOfType.values()) {
            Consumers.setConsumer(value);
        }
    }

}
