package com.github.binarybeing.easyevent.model;

import java.lang.reflect.Method;

/**
 * @Description
 * @Author binarybeing
 * @Date 2021-02-09
 **/
public class AroundMethod {

    private Object targetObject;

    private Method methodInfo;

    private Object[] params;

    private Exception exp;

    private Object returnObj;

    private AroundMethod(Method method, Object targetObject) {
        this.methodInfo = method;
        this.targetObject = targetObject;
    }

    public static AroundMethod build(Method method, Object targetObject) {
        return new AroundMethod(method, targetObject);
    }

    public AroundMethod withParams(Object... params) {
        this.params = params;
        return this;
    }

    public AroundMethod withException(Exception e) {
        this.exp = e;
        return this;
    }

    public Object getReturnObj() {
        return returnObj;
    }

    public AroundMethod withReturnObj(Object returnObj) {
        this.returnObj = returnObj;
        return this;
    }

    public Object getTargetObject() {
        return targetObject;
    }

    public Method getMethodInfo() {
        return methodInfo;
    }

    public Object[] getParams() {
        return params;
    }

    public Exception getExp() {
        return exp;
    }
}
