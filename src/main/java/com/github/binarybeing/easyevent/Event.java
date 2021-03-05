package com.github.binarybeing.easyevent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description
 * @Author binarybeing
 * @Date 2021-02-09
 **/
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
public @interface Event {

    Class<? extends EventConsumer>[] value();

}
