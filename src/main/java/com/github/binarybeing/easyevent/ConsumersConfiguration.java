package com.github.binarybeing.easyevent;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @Description
 * @Author junhua5
 * @Date 2021-03-06
 **/
public class ConsumersConfiguration implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Consumers.setApplicationContext(applicationContext);
    }

}
