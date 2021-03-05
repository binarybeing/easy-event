package com.github.binarybeing.easyevent;

import com.github.binarybeing.easyevent.model.AroundMethod;

/**
 * @Description
 * @Author binarybeing
 * @Date 2021-02-09
 **/
public interface EventConsumer {

    void before(AroundMethod targetMethod);

    void after(AroundMethod targetMethod);

    void thrown(AroundMethod targetMethod, Exception e);

}
