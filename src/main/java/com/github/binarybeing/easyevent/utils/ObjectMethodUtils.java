package com.github.binarybeing.easyevent.utils;

import org.apache.log4j.Logger;

import java.lang.reflect.Method;

/**
 * @Description
 * @Author binarybeing
 * @Date 2021-02-18
 **/
public class ObjectMethodUtils {

    private static Logger errorLogger = Logger.getLogger(ObjectMethodUtils.class);

    public static Method getMethod(Class clazz, String methodName, Class... paramClasses) {
        try {
            return clazz.getDeclaredMethod(methodName, paramClasses);
        } catch (NoSuchMethodException e) {
            errorLogger.error("ObjectMethodUtils getMethod error", e);
        }
        return null;
    }
}
