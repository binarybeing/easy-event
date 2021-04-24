package com.github.binarybeing.easyevent.processor;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @Description
 * @Author binarybeing
 * @Date 2021-02-14
 **/
class AnnotatedMethodInfo {

    private String eventName;

    private JCTree.JCMethodDecl methodDecl;

    private JCTree.JCClassDecl classDecl;

    private List<Symbol.ClassSymbol> enhancerClasses = new ArrayList<>();

    public List<Symbol.ClassSymbol> getEnhancerClasses() {
        return enhancerClasses;
    }

    public void addEnhancerClasses(Symbol.ClassSymbol enhancerClass) {
        enhancerClasses.add(enhancerClass);
    }

    public JCTree.JCMethodDecl getMethodDecl() {
        return methodDecl;
    }

    public void setMethodDecl(JCTree.JCMethodDecl methodDecl) {
        this.methodDecl = methodDecl;
    }

    public JCTree.JCClassDecl getClassDecl() {
        return classDecl;
    }

    public void setClassDecl(JCTree.JCClassDecl classDecl) {
        this.classDecl = classDecl;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AnnotatedMethodInfo that = (AnnotatedMethodInfo) o;
        return Objects.equals(getMethodDecl(), that.getMethodDecl()) && Objects.equals(getClassDecl(), that.getClassDecl())
                && Objects.equals(getEnhancerClasses(), that.getEnhancerClasses());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMethodDecl(), getClassDecl(), getEnhancerClasses());
    }
}
