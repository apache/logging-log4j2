/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.config.plugins.processor;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import org.apache.logging.log4j.core.config.plugins.processor.internal.Annotations;
import org.apache.logging.log4j.core.config.plugins.processor.internal.ReachabilityMetadata;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.Nullable;

/**
 * Java annotation processor that generates GraalVM metadata.
 * <p>
 *     <strong>Note:</strong> The annotations listed here must also be classified by the {@link Annotations} helper.
 * </p>
 */
@ServiceProvider(value = Processor.class, resolution = Resolution.OPTIONAL)
@SupportedAnnotationTypes({
    "org.apache.logging.log4j.core.config.plugins.validation.Constraint",
    "org.apache.logging.log4j.core.config.plugins.Plugin",
    "org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute",
    "org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory",
    "org.apache.logging.log4j.core.config.plugins.PluginConfiguration",
    "org.apache.logging.log4j.core.config.plugins.PluginElement",
    "org.apache.logging.log4j.core.config.plugins.PluginFactory",
    "org.apache.logging.log4j.core.config.plugins.PluginLoggerContext",
    "org.apache.logging.log4j.core.config.plugins.PluginNode",
    "org.apache.logging.log4j.core.config.plugins.PluginValue",
    "org.apache.logging.log4j.core.config.plugins.PluginVisitorStrategy"
})
@SupportedOptions({"log4j.graalvm.groupId", "log4j.graalvm.artifactId"})
public class GraalVmProcessor extends AbstractProcessor {

    private static final String GROUP_ID = "log4j.graalvm.groupId";
    private static final String ARTIFACT_ID = "log4j.graalvm.artifactId";
    private static final String PROCESSOR_NAME = GraalVmProcessor.class.getSimpleName();

    private final Map<String, ReachabilityMetadata.Type> reachableTypes = new HashMap<>();
    private final List<Element> processedElements = new ArrayList<>();
    private Annotations annotationUtil;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.annotationUtil = new Annotations(processingEnv.getElementUtils());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();
        for (TypeElement annotation : annotations) {
            Annotations.Type annotationType = annotationUtil.classifyAnnotation(annotation);
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                switch (annotationType) {
                    case PLUGIN:
                        processPlugin(element);
                        break;
                    case CONSTRAINT_OR_VISITOR:
                        processConstraintOrVisitor(element, annotation);
                        break;
                    case PARAMETER:
                        processParameter(element);
                        break;
                    case FACTORY:
                        processFactory(element);
                        break;
                    case UNKNOWN:
                        messager.printMessage(
                                Diagnostic.Kind.WARNING,
                                String.format(
                                        "The annotation type `%s` is not handled by %s", annotation, PROCESSOR_NAME),
                                annotation);
                }
                processedElements.add(element);
            }
        }
        // Write the result file
        if (roundEnv.processingOver() && !reachableTypes.isEmpty()) {
            writeReachabilityMetadata();
        }
        // Do not claim the annotations to allow other annotation processors to run
        return false;
    }

    private void processPlugin(Element element) {
        TypeElement typeElement = safeCast(element, TypeElement.class);
        for (Element child : typeElement.getEnclosedElements()) {
            if (child instanceof ExecutableElement) {
                ExecutableElement executableChild = (ExecutableElement) child;
                if (executableChild.getModifiers().contains(Modifier.PUBLIC)) {
                    switch (executableChild.getSimpleName().toString()) {
                            // 1. All public constructors.
                        case "<init>":
                            addMethod(typeElement, executableChild);
                            break;
                            // 2. Static `newInstance` method used in, e.g. `PatternConverter` classes.
                        case "newInstance":
                            if (executableChild.getModifiers().contains(Modifier.STATIC)) {
                                addMethod(typeElement, executableChild);
                            }
                            break;
                            // 3. Other factory methods are annotated, so we don't deal with them here.
                        default:
                    }
                }
            }
        }
    }

    private void processConstraintOrVisitor(Element element, TypeElement annotation) {
        // Add the metadata for the public constructors
        processPlugin(annotationUtil.getAnnotationClassValue(element, annotation));
    }

    private void processParameter(Element element) {
        switch (element.getKind()) {
            case FIELD:
                addField(
                        safeCast(element.getEnclosingElement(), TypeElement.class),
                        safeCast(element, VariableElement.class));
                break;
            case PARAMETER:
                // Do nothing, the containing method must be annotated with a factory annotation.
                break;
            default:
                String msg = String.format("Invalid Log4j parameter element `%s`.", element);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element);
                throw new IllegalStateException(msg);
        }
    }

    private void processFactory(Element element) {
        addMethod(
                safeCast(element.getEnclosingElement(), TypeElement.class), safeCast(element, ExecutableElement.class));
    }

    private void writeReachabilityMetadata() {
        //
        // Many users will have `log4j-core` on the annotation processor path, but do not have Log4j Plugins.
        // Therefore, we check for the annotation processor required options only if some elements were processed.
        //
        String reachabilityMetadataPath = getReachabilityMetadataPath();
        Messager messager = processingEnv.getMessager();
        messager.printMessage(
                Diagnostic.Kind.NOTE,
                String.format(
                        "%s: writing GraalVM metadata for %d Java classes to `%s`.",
                        PROCESSOR_NAME, reachableTypes.size(), reachabilityMetadataPath));
        try (OutputStream output = processingEnv
                .getFiler()
                .createResource(
                        StandardLocation.CLASS_OUTPUT,
                        Strings.EMPTY,
                        reachabilityMetadataPath,
                        processedElements.toArray(new Element[0]))
                .openOutputStream()) {
            ReachabilityMetadata.writeReflectConfig(reachableTypes.values(), output);
        } catch (IOException e) {
            String message = String.format(
                    "%s: unable to write reachability metadata to file `%s`", PROCESSOR_NAME, reachabilityMetadataPath);
            messager.printMessage(Diagnostic.Kind.ERROR, message);
            throw new IllegalArgumentException(message, e);
        }
    }

    private String getReachabilityMetadataPath() {
        String groupId = processingEnv.getOptions().get(GROUP_ID);
        String artifactId = processingEnv.getOptions().get(ARTIFACT_ID);
        if (groupId == null || artifactId == null) {
            String message = String.format(
                    "The `%s` annotation processor is missing the required `%s` and `%s` options.%n"
                            + "The generation of GraalVM reflection metadata for your Log4j Plugins will be disabled.",
                    PROCESSOR_NAME, GROUP_ID, ARTIFACT_ID);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
            throw new IllegalArgumentException(message);
        }
        return String.format("META-INF/native-image/%s/%s/reflect-config.json", groupId, artifactId);
    }

    private void addField(TypeElement parent, VariableElement element) {
        ReachabilityMetadata.Type reachableType =
                reachableTypes.computeIfAbsent(toString(parent), ReachabilityMetadata.Type::new);
        reachableType.addField(
                new ReachabilityMetadata.Field(element.getSimpleName().toString()));
    }

    private void addMethod(TypeElement parent, ExecutableElement element) {
        ReachabilityMetadata.Type reachableType =
                reachableTypes.computeIfAbsent(toString(parent), ReachabilityMetadata.Type::new);
        ReachabilityMetadata.Method method =
                new ReachabilityMetadata.Method(element.getSimpleName().toString());
        element.getParameters().stream().map(v -> toString(v.asType())).forEach(method::addParameterType);
        reachableType.addMethod(method);
    }

    private <T extends Element> T safeCast(Element element, Class<T> type) {
        if (type.isInstance(element)) {
            return type.cast(element);
        }
        // This should never happen, unless annotations start appearing on unexpected elements.
        String msg = String.format(
                "Unexpected type of element `%s`: expecting `%s` but found `%s`",
                element, type.getName(), element.getClass().getName());
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element);
        throw new IllegalStateException(msg);
    }

    /**
     * Returns the fully qualified name of a type.
     *
     * @param type A Java type.
     */
    private String toString(TypeMirror type) {
        return type.accept(
                new SimpleTypeVisitor8<String, @Nullable Void>() {
                    @Override
                    protected String defaultAction(final TypeMirror e, @Nullable Void unused) {
                        return e.toString();
                    }

                    @Override
                    public String visitArray(final ArrayType t, @Nullable Void unused) {
                        return visit(t.getComponentType(), unused) + "[]";
                    }

                    @Override
                    public @Nullable String visitDeclared(final DeclaredType t, final Void unused) {
                        return processingEnv.getTypeUtils().erasure(t).toString();
                    }
                },
                null);
    }

    /**
     * Returns the fully qualified name of the element corresponding to a {@link DeclaredType}.
     *
     * @param element A Java language element.
     */
    private String toString(Element element) {
        return element.accept(
                new SimpleElementVisitor8<String, @Nullable Void>() {
                    @Override
                    public String visitPackage(PackageElement e, @Nullable Void unused) {
                        return e.getQualifiedName().toString();
                    }

                    @Override
                    public String visitType(TypeElement e, @Nullable Void unused) {
                        Element parent = e.getEnclosingElement();
                        String separator = parent.getKind() == ElementKind.PACKAGE ? "." : "$";
                        return visit(parent, unused)
                                + separator
                                + e.getSimpleName().toString();
                    }

                    @Override
                    protected String defaultAction(Element e, @Nullable Void unused) {
                        return "";
                    }
                },
                null);
    }
}
