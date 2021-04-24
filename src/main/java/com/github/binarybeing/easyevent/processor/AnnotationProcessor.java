package com.github.binarybeing.easyevent.processor;

import com.github.binarybeing.easyevent.Consumers;
import com.github.binarybeing.easyevent.Event;
import com.github.binarybeing.easyevent.model.AroundMethod;
import com.github.binarybeing.easyevent.utils.ObjectMethodUtils;
import com.google.auto.service.AutoService;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description
 * @Author binarybeing
 * @Date 2021-02-09
 **/
@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {

    private Types typeUtils = null;
    private JavacElements elementUtils = null;
    private Filer filer = null;
    private Messager messager = null;
    private TreeMaker treeMaker = null;
    private String METHOD_TARGET = "$methodTarget";
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.typeUtils = processingEnv.getTypeUtils();
        this.elementUtils = (JavacElements)processingEnv.getElementUtils();
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        final Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        names = Names.instance(context);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Sets.newHashSet(Event.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        messager.printMessage(Diagnostic.Kind.NOTE, "AnnotationProcessor start");
        Set<AnnotatedMethodInfo> target = this.getAopTarget(roundEnv);
        if (CollectionUtils.isEmpty(target)) {
            return false;
        }
        for (AnnotatedMethodInfo annotatedMethodInfo : target) {
            JCTree.JCMethodDecl methodDecl = annotatedMethodInfo.getMethodDecl();
            JCTree.JCClassDecl classDecl = annotatedMethodInfo.getClassDecl();
            if (classDecl == null || classDecl.sym == null || classDecl.sym.isInterface()) {
                continue;
            }
            treeMaker.pos = methodDecl.pos;
            JCTree.JCMethodDecl jcMethodDeclProxy = this.genAopTargetMethod(methodDecl);
            classDecl.defs = classDecl.defs.append(jcMethodDeclProxy);

            JCTree returnType = methodDecl.getReturnType();
            boolean notVoidReturn = !"void".equals(returnType.toString());

            List<Symbol.ClassSymbol> enhancerClasses = List.from(annotatedMethodInfo.getEnhancerClasses());

            String theMethodReference = this.createMethodInfo(methodDecl, classDecl);
            String consumerListReference = this.createMethodConsumerList(methodDecl, classDecl, enhancerClasses);

            JCTree.JCBlock tryBlock = this.tryBlock(notVoidReturn, methodDecl, consumerListReference);
            JCTree.JCCatch catchBlock = this.catchBlock(consumerListReference);
            JCTree.JCBlock finalBlock = this.finalBlock(notVoidReturn, consumerListReference);

            JCTree.JCTry jcTry = treeMaker.Try(List.nil(), tryBlock, List.of(catchBlock), finalBlock);

            if (notVoidReturn) {
                methodDecl.body = treeMaker.Block(0, List.of(
                        this.methodInfoBuilder(annotatedMethodInfo, theMethodReference),
                        this.createReturnObj(),
                        jcTry,
                        this.returnResult(returnType))
                );
            }else {
                methodDecl.body = treeMaker.Block(0, List.of(
                        this.methodInfoBuilder(annotatedMethodInfo, theMethodReference),
                        jcTry)
                );
            }

        }
        return true;
    }

    private AtomicInteger consumerListCounter = new AtomicInteger(0);
    private String createMethodConsumerList(JCTree.JCMethodDecl methodDecl, JCTree.JCClassDecl classDecl, List<Symbol.ClassSymbol> enhancerClasses) {
        String consumerListReference = classDecl.getSimpleName() + "_" + methodDecl.getName().toString()+ "_consumers_" + consumerListCounter.incrementAndGet();

        List<String> newArrayListMethod = this.classMethodStringList(Lists.class, "newArrayList");

        ArrayList<JCTree.JCExpression> params = new ArrayList<>();

        for (Symbol.ClassSymbol enhancerClass : enhancerClasses) {
            JCTree.JCMethodInvocation getConsumer =
                    treeMaker.Apply(null, this.invokeMethodExpression(this.classMethodStringList(Consumers.class, "getConsumer")), List.of(treeMaker.ClassLiteral(enhancerClass.type)));
            params.add(getConsumer);
        }

        JCTree.JCMethodInvocation methodInvocation = treeMaker.Apply(null, this.invokeMethodExpression(newArrayListMethod), List.from(params));
        JCTree.JCVariableDecl methodVarDef =
                treeMaker.VarDef(treeMaker.Modifiers(Flags.STATIC), elementUtils.getName(consumerListReference), this.generateJcExpression(java.util.List.class.getCanonicalName()),
                        //变量值
                        methodInvocation);
        classDecl.defs = classDecl.defs.append(methodVarDef);
        return consumerListReference;
    }

    private AtomicInteger methodCounter = new AtomicInteger(0);
    private String createMethodInfo(JCTree.JCMethodDecl methodDecl, JCTree.JCClassDecl classDecl) {
        List<String> getMethod = this.classMethodStringList(ObjectMethodUtils.class, "getMethod");
        ArrayList<JCTree.JCExpression> params = new ArrayList<>();

        String methodReferenceName = classDecl.getSimpleName() + "_" + methodDecl.getName().toString() +"_"+ methodCounter.incrementAndGet();
        params.add(this.getClassParam(classDecl.sym.type));
        params.add(treeMaker.Literal(methodDecl.name.toString()));
        List<JCTree.JCVariableDecl> parameters = methodDecl.getParameters();
        for (JCTree.JCVariableDecl parameter : parameters) {
            params.add(this.getClassParam(parameter.sym.type));
        }

        JCTree.JCMethodInvocation methodInvocation = treeMaker.Apply(null, this.invokeMethodExpression(getMethod), List.from(params));
        JCTree.JCVariableDecl methodVarDef =
                treeMaker.VarDef(treeMaker.Modifiers(Flags.STATIC), elementUtils.getName(methodReferenceName), this.generateJcExpression(Method.class.getCanonicalName()),
                        //变量值
                        methodInvocation);

        classDecl.defs = classDecl.defs.append(methodVarDef);
        return methodReferenceName;
    }

    private JCTree.JCExpression getClassParam(Type type) {
        if (type.isPrimitive()) {
            return treeMaker.ClassLiteral(type);
        }
        return memberAccess(type.tsym + ".class");
    }
    private JCTree.JCExpression memberAccess(String components) {
        String[] componentArray = components.split("\\.");
        JCTree.JCExpression expr = treeMaker.Ident(names.fromString(componentArray[0]));
        for (int i = 1; i < componentArray.length; i++) {
            expr = treeMaker.Select(expr, names.fromString(componentArray[i]));
        }
        return expr;
    }

    private JCTree.JCVariableDecl createReturnObj() {
        JCTree.JCLiteral literal = treeMaker.Literal(TypeTag.BOT, null);

        return treeMaker.VarDef(
                treeMaker.Modifiers(Flags.BLOCK),
                elementUtils.getName("return_obj"),
                this.generateJcExpression(Object.class.getCanonicalName()),
                //变量值
                literal);
    }

    private JCTree.JCReturn returnResult(JCTree returnType) {
        return treeMaker.Return(treeMaker.TypeCast(returnType, treeMaker.Ident(elementUtils.getName("return_obj"))));
    }

    private JCTree.JCMethodDecl genAopTargetMethod(JCTree.JCMethodDecl target){
        JCTree.JCModifiers jcModifiers = treeMaker.Modifiers(Flags.PRIVATE);
        JCTree.JCExpression returnType = (JCTree.JCExpression)target.getReturnType();
        Name name = elementUtils.getName("$_$" + target.getName());
        //构建代码块
        JCTree.JCBlock jcBlock = target.body;
        //泛型参数列表
        List<JCTree.JCTypeParameter> methodGenericParams = target.typarams;
        //参数列表
        List<JCTree.JCVariableDecl> parameters = target.params;
        ArrayList<JCTree.JCVariableDecl> newParameters = Lists.newArrayListWithExpectedSize(parameters.size());
        for (JCTree.JCVariableDecl parameter : parameters) {
            JCTree.JCVariableDecl varDef = treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER), parameter.name, parameter.vartype, null);
            newParameters.add(varDef);
        }
        parameters = List.from(newParameters);
        //异常抛出列表
        List<JCTree.JCExpression> throwsClauses = target.thrown;
        //非自定义注解类中的方法，defaultValue为null
        JCTree.JCExpression defaultValue = null;
        //最后构建方法
        JCTree.JCMethodDecl jcMethodDecl = treeMaker.MethodDef(jcModifiers, name, returnType, methodGenericParams, parameters, throwsClauses, jcBlock, defaultValue);
        return jcMethodDecl;
    }

    /**
     * 定义 AroundMethod
     *
     * @return
     */
    private JCTree.JCVariableDecl methodInfoBuilder(AnnotatedMethodInfo annotatedMethodInfo, String theMethodReference) {
        JCTree.JCMethodDecl methodDecl = annotatedMethodInfo.getMethodDecl();
        JCTree.JCClassDecl classDecl = annotatedMethodInfo.getClassDecl();
        String eventName = annotatedMethodInfo.getEventName();

        ArrayList<JCTree.JCExpression> buildParams = new ArrayList<>();
        buildParams.add(treeMaker.Ident(elementUtils.getName(theMethodReference)));
        buildParams.add(treeMaker.This(classDecl.sym.type));

        ArrayList<JCTree.JCExpression> withParamsParams = new ArrayList<>();

        ArrayList<JCTree.JCExpression> paramNames = new ArrayList<>();
        for (JCTree.JCVariableDecl parameter : methodDecl.getParameters()) {
            withParamsParams.add(treeMaker.Ident(parameter.name));
            paramNames.add(treeMaker.Literal(parameter.name.toString()));
        }
        List<String> aroundMethodBuilder = this.classMethodStringList(AroundMethod.class, "build");
        JCTree.JCExpression chainMethodInvocation =
                this.getChainMethodInvocation(Pair.of(aroundMethodBuilder, List.from(buildParams)),
                        Pair.of(List.of("withEventName"), List.of(treeMaker.Literal(eventName==null?"":eventName))),
                        Pair.of(List.of("withParamsName"), List.from(paramNames)),
                        Pair.of(List.of("withParams"), List.from(withParamsParams)));
        return treeMaker.VarDef(
                //访问修饰符
                treeMaker.Modifiers(Flags.FINAL),
                //变量名称
                elementUtils.getName(METHOD_TARGET),
                //变量类型
                this.generateJcExpression(AroundMethod.class.getCanonicalName()),
                //变量值
                chainMethodInvocation);
    }


    private JCTree.JCBlock tryBlock(boolean notVoidReturn, JCTree.JCMethodDecl methodDecl, String consumerListReference) {
        ArrayList<JCTree.JCStatement> jcStatements = new ArrayList<>();
        JCTree.JCIdent targetMethod = treeMaker.Ident(elementUtils.getName(METHOD_TARGET));


        List<String> invokeBefore = this.classMethodStringList(Consumers.class, "invokeBefore");
        JCTree.JCIdent consumerListIdent = treeMaker.Ident(elementUtils.getName(consumerListReference));
        JCTree.JCMethodInvocation invocation = treeMaker.Apply(null, this.invokeMethodExpression(invokeBefore), List.of(consumerListIdent, targetMethod));
        jcStatements.add(treeMaker.Exec(invocation));

        ArrayList<JCTree.JCExpression> param = new ArrayList<>();
        for (JCTree.JCVariableDecl variableDecl : methodDecl.getParameters()) {
            param.add(treeMaker.Ident(elementUtils.getName(variableDecl.name.toString())));
        }

        JCTree.JCIdent ident = treeMaker.Ident(elementUtils.getName("$_$" + methodDecl.getName()));
        JCTree.JCExpressionStatement expressionStatement;
        if (notVoidReturn) {
            expressionStatement =
                    treeMaker.Exec(treeMaker.Assign(treeMaker.Ident(elementUtils.getName("return_obj")), treeMaker.Apply(null, ident, List.from(param))));
        } else {
            expressionStatement = treeMaker.Exec(treeMaker.Apply(null, ident, List.from(param)));
        }
        jcStatements.add(expressionStatement);
        return treeMaker.Block(0, List.from(jcStatements));
    }
    private JCTree.JCCatch catchBlock(String consumerListReference) {
        ArrayList<JCTree.JCStatement> jcStatements = new ArrayList<>();

        JCTree.JCIdent targetMethod = treeMaker.Ident(elementUtils.getName(METHOD_TARGET));
        JCTree.JCIdent exception = treeMaker.Ident(elementUtils.getName("e"));

        List<String> invokeBefore = this.classMethodStringList(Consumers.class, "invokeThrown");
        JCTree.JCIdent consumerListIdent = treeMaker.Ident(elementUtils.getName(consumerListReference));
        JCTree.JCMethodInvocation invocation =
                treeMaker.Apply(null, this.invokeMethodExpression(invokeBefore), List.of(consumerListIdent, targetMethod, exception));
        jcStatements.add(treeMaker.Exec(invocation));

        JCTree.JCThrow jcThrow = treeMaker.Throw(treeMaker.Ident(elementUtils.getName("e")));
        jcStatements.add(jcThrow);

        final JCTree.JCVariableDecl e = treeMaker.VarDef(treeMaker.Modifiers(0), elementUtils.getName("e"), treeMaker.Ident(elementUtils.getName("Exception")), null);
        JCTree.JCBlock block = treeMaker.Block(0, List.from(jcStatements));
        return treeMaker.Catch(e, block);
    }

    private JCTree.JCBlock finalBlock(boolean notVoidReturn, String consumerListReference) {
        ArrayList<JCTree.JCStatement> jcStatements = new ArrayList<>();
        if (notVoidReturn) {
            JCTree.JCMethodInvocation apply = this.
                    treeMaker.Apply(null, this.invokeMethodExpression(List.of(METHOD_TARGET, "withReturnObj")), List.of(treeMaker.Ident(elementUtils.getName("return_obj"))));
            jcStatements.add(treeMaker.Exec(apply));
        }

        List<String> invokeBefore = this.classMethodStringList(Consumers.class, "invokeAfter");
        JCTree.JCIdent consumerListIdent = treeMaker.Ident(elementUtils.getName(consumerListReference));
        JCTree.JCIdent targetMethod = treeMaker.Ident(elementUtils.getName(METHOD_TARGET));

        JCTree.JCMethodInvocation invocation =
                treeMaker.Apply(null, this.invokeMethodExpression(invokeBefore), List.of(consumerListIdent, targetMethod));
        jcStatements.add(treeMaker.Exec(invocation));

        return treeMaker.Block(0, List.from(jcStatements));
    }

    private JCTree.JCMethodInvocation getChainMethodInvocation(Pair<List<String>, List<JCTree.JCExpression>>...statements) {
        JCTree.JCMethodInvocation invocation = null;
        for (Pair<List<String>, List<JCTree.JCExpression>> statement : statements) {
            List<String> statementsContent = statement.getKey();
            List<JCTree.JCExpression> params = statement.getValue();
            if (null == invocation) {
                invocation = treeMaker.Apply(null, this.invokeMethodExpression(statementsContent), params);
                continue;
            }
            String methodName = statementsContent.get(0);
            JCTree.JCFieldAccess method = treeMaker.Select(invocation, elementUtils.getName(methodName));
            invocation = treeMaker.Apply(null, method, params);
        }
        return invocation;
    }

    /**
     * 输入示例 [com,weibo,api,live,util,logger,LiveLogger,withBiz]
     * com.weibo.api.live.util.logger.LiveLogger
     * com.weibo.api.live.util.logger.LiveLogger
     *
     * @param names
     * @return
     */
    private JCTree.JCExpression invokeMethodExpression(final List<String> names) {
        JCTree.JCExpression expression = null;
        JCTree.JCIdent ident = null;
        for (String name : names) {
            if (ident == null) {
                ident = treeMaker.Ident(elementUtils.getName(name));
                continue;
            }
            if (expression == null) {
                expression = treeMaker.Select(ident, elementUtils.getName(name));
                continue;
            }
            expression = treeMaker.Select(expression, elementUtils.getName(name));
        }
        return expression;
    }
    /**
     * 根据类全路径名称，获取类全名对应的Expression
     * @param fullNameOfTheClass
     * @return
     */
    private JCTree.JCExpression generateJcExpression(final String fullNameOfTheClass) {

        String[] fullNameOfTheClassArray = fullNameOfTheClass.split("\\.");

        JCTree.JCExpression expr = treeMaker.Ident(elementUtils.getName(fullNameOfTheClassArray[0]));
        for (int i = 1; i < fullNameOfTheClassArray.length; i++) {
            expr = treeMaker.Select(expr, elementUtils.getName(fullNameOfTheClassArray[i]));
        }
        return expr;
    }

    private Set<AnnotatedMethodInfo> getAopTarget(RoundEnvironment roundEnv){

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Event.class);
        if (CollectionUtils.isEmpty(elements)) {
            return Collections.emptySet();
        }
        Set<AnnotatedMethodInfo> result = new HashSet<>();

        for (Element element : elements) {
            Element enclosingElement = element.getEnclosingElement();
            JCTree tree = elementUtils.getTree(enclosingElement);
            JCTree.JCClassDecl jcClassDecl = null;
            if (tree instanceof JCTree.JCClassDecl) {
                jcClassDecl = (JCTree.JCClassDecl) tree;
            }
            JCTree member = elementUtils.getTree(element);
            if (member instanceof JCTree.JCMethodDecl) {
                JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) member;
                java.util.List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
                for (AnnotationMirror mirror : annotationMirrors) {
                    DeclaredType type = mirror.getAnnotationType();
                    if (!type.toString().equals(Event.class.getCanonicalName())) {
                         continue;
                    }

                    AnnotatedMethodInfo annotatedMethodInfo = new AnnotatedMethodInfo();
                    annotatedMethodInfo.setMethodDecl(methodDecl);
                    annotatedMethodInfo.setClassDecl(jcClassDecl);
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                        ExecutableElement key = entry.getKey();
                        if ("consumers".equals(key.getSimpleName().toString())) {
                            Attribute.Array clazzArray = (Attribute.Array)entry.getValue();
                            for (Attribute value : clazzArray.values) {
                                Attribute.Class attribute = (Attribute.Class) value;
                                Type classType = attribute.classType;
                                Symbol.ClassSymbol clazzElement = (Symbol.ClassSymbol)typeUtils.asElement(classType);
                                annotatedMethodInfo.addEnhancerClasses(clazzElement);
                            }
                        }
                        if ("name".equals(key.getSimpleName().toString())) {
                            AnnotationValue value = entry.getValue();
                            annotatedMethodInfo.setEventName(value.getValue().toString());
                        }
                    }
                    result.add(annotatedMethodInfo);
                }
            }
        }
        return result;
    }

    private List<String> classMethodStringList(Class clazz, String method) {
        String canonicalName = clazz.getCanonicalName();
        String[] split = canonicalName.split("\\.");

        String[] strings = new String[split.length + 1];
        for (int i = 0; i < split.length; i++) {
            strings[i] = split[i];
        }
        strings[split.length] = method;
        return List.from(strings);
    }

}
