package com.github.binarybeing.easyevent;

import com.github.binarybeing.easyevent.model.AroundMethod;

import java.util.concurrent.Executor;

/**
 * @Description
 * @Author binarybeing
 * @Date 2021-02-09
 **/
public interface EventConsumer {

    /**
     * 事件之前
     * @param targetMethod
     */
    void before(AroundMethod targetMethod);

    /**
     * 事件之后
     * @param targetMethod
     */
    void after(AroundMethod targetMethod);

    /**
     * 事件异常
     * @param targetMethod
     * @param e
     */
    void thrown(AroundMethod targetMethod, Exception e);

    /**
     * consumer 任务执行器
     * @return Executor
     */
    Executor executor();
}
