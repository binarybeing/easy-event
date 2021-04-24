package com.github.binarybeing.easyevent.model;

import com.google.common.collect.Lists;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author binarybeing
 * @Date 2021-02-09
 **/
public class AroundMethod {

    private Object targetObject;

    private Method methodInfo;

    private MethodParam methodParam;

    private Exception exp;

    private Object returnObj;

    private String eventName;

    private Map<String, Object> contextMap;

    private AroundMethod(Method method, Object targetObject) {
        this.methodInfo = method;
        this.targetObject = targetObject;
        this.methodParam = new MethodParam();
        methodParam.parameters = methodInfo.getParameters();
    }

    public static AroundMethod build(Method method, Object targetObject) {
        return new AroundMethod(method, targetObject);
    }

    public AroundMethod withParams(Object... params) {
        methodParam.params = params;
        return this;
    }
    public AroundMethod withParamsName(String... paramName) {
        methodParam.names = paramName;
        return this;
    }

    public AroundMethod withEventName(String eventName) {
        this.eventName = eventName;
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

    public Exception getExp() {
        return exp;
    }

    public String getEventName() {
        return eventName;
    }

    public Map<String, Object> getContextMap() {
        return contextMap;
    }

    public <T> T getContextObject(String key, Class<T> clazz) {
        if (contextMap == null) {
            return null;
        }
        return (T) contextMap.get(key);
    }

    public void setContextMap(Map<String, Object> contextMap) {
        this.contextMap = contextMap;
    }

    public MethodParam getMethodParam() {
        return methodParam;
    }

    public class MethodParam {
        private Object[] params;
        private Parameter[] parameters;
        private String[] names;

        public List<Param> toList() {
            List<Param> result = Lists.newArrayListWithExpectedSize(parameters.length);
            for (int i = 0; i < parameters.length; i++) {
                Param param = new Param();
                param.setName(names[i]);
                param.setParam(params[i]);
                param.setParameter(parameters[i]);
                result.add(param);
            }
            return result;
        }

        public Object[] getParams() {
            return params;
        }

        public void setParams(Object[] params) {
            this.params = params;
        }

        public Parameter[] getParameters() {
            return parameters;
        }

        public void setParameters(Parameter[] parameters) {
            this.parameters = parameters;
        }

        public String[] getNames() {
            return names;
        }

        public void setNames(String[] names) {
            this.names = names;
        }

    }


    public class Param {
        private Object param;
        private Parameter parameter;
        private String name;

        public Object getParam() {
            return param;
        }

        public void setParam(Object param) {
            this.param = param;
        }

        public Parameter getParameter() {
            return parameter;
        }

        public void setParameter(Parameter parameter) {
            this.parameter = parameter;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


}
