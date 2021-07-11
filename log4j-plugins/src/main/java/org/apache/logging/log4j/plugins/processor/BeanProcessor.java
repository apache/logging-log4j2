/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.apache.logging.log4j.plugins.processor;

import org.apache.logging.log4j.plugins.di.Disposes;
import org.apache.logging.log4j.plugins.di.Inject;
import org.apache.logging.log4j.plugins.di.Producer;
import org.apache.logging.log4j.plugins.di.Qualifier;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementKindVisitor9;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor9;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: migrate to separate maven module between log4j-plugins and log4j-core
@SupportedAnnotationTypes({"org.apache.logging.log4j.plugins.*", "org.apache.logging.log4j.core.config.plugins.*"})
@SupportedOptions("pluginPackage")
public class BeanProcessor extends AbstractProcessor {
    public static final String BEAN_INFO_SERVICE_FILE = "META-INF/services/org.apache.logging.log4j.plugins.di.spi.BeanInfoService";

    public BeanProcessor() {
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private static class ProducerAnnotationVisitor extends ElementKindVisitor9<Void, Void> {
        private final Set<ExecutableElement> producerMethods = new HashSet<>();
        private final Set<VariableElement> producerFields = new HashSet<>();

        @Override
        public Void visitVariableAsField(final VariableElement e, final Void unused) {
            producerFields.add(e);
            return null;
        }

        @Override
        public Void visitExecutableAsMethod(final ExecutableElement e, final Void unused) {
            producerMethods.add(e);
            return null;
        }
    }

    private static class DisposesAnnotationVisitor extends ElementKindVisitor9<Void, Void> {
        private final Set<ExecutableElement> disposesMethods = new HashSet<>();

        @Override
        public Void visitVariableAsParameter(final VariableElement e, final Void unused) {
            disposesMethods.add((ExecutableElement) e.getEnclosingElement());
            return null;
        }
    }

    private static class InjectAnnotationVisitor extends ElementKindVisitor9<Void, Void> {
        private final Set<TypeElement> injectableClasses = new HashSet<>();

        @Override
        public Void visitVariableAsField(final VariableElement e, final Void unused) {
            injectableClasses.add((TypeElement) e.getEnclosingElement());
            return null;
        }

        @Override
        public Void visitExecutableAsConstructor(final ExecutableElement e, final Void unused) {
            injectableClasses.add((TypeElement) e.getEnclosingElement());
            return null;
        }

        @Override
        public Void visitExecutableAsMethod(final ExecutableElement e, final Void unused) {
            injectableClasses.add((TypeElement) e.getEnclosingElement());
            return null;
        }
    }

    private static class QualifiedAnnotationVisitor extends ElementKindVisitor9<Void, Void> {
        private final Predicate<AnnotationMirror> isProducerAnnotation;
        private final Set<TypeElement> injectableClasses = new HashSet<>();

        private QualifiedAnnotationVisitor(final Predicate<AnnotationMirror> isProducerAnnotation) {
            this.isProducerAnnotation = isProducerAnnotation;
        }

        @Override
        public Void visitVariableAsField(final VariableElement e, final Void unused) {
            if (e.getAnnotationMirrors().stream().noneMatch(isProducerAnnotation)) {
                injectableClasses.add((TypeElement) e.getEnclosingElement());
            }
            return null;
        }

        @Override
        public Void visitVariableAsParameter(final VariableElement e, final Void unused) {
            final Element enclosingExecutable = e.getEnclosingElement();
            final TypeElement typeElement = (TypeElement) enclosingExecutable.getEnclosingElement();
            if (enclosingExecutable.getKind() == ElementKind.CONSTRUCTOR ||
                    enclosingExecutable.getAnnotationMirrors().stream().noneMatch(isProducerAnnotation)) {
                injectableClasses.add(typeElement);
            }
            return null;
        }
    }

    private static class PluginAnnotationVisitor extends ElementKindVisitor9<Void, Void> {
        private final Map<String, Set<TypeElement>> pluginCategories = new HashMap<>();

        @Override
        public Void visitTypeAsClass(final TypeElement e, final Void unused) {
            final AnnotationMirror pluginAnnotation = e.getAnnotationMirrors()
                    .stream()
                    .filter(ann -> ann.getAnnotationType().asElement().getSimpleName().contentEquals("Plugin"))
                    .findAny()
                    .orElseThrow();
            final ExecutableElement categoryKey = pluginAnnotation.getElementValues()
                    .keySet()
                    .stream()
                    .filter(element -> element.getSimpleName().contentEquals("category"))
                    .findAny()
                    .orElseThrow();

            final String category = pluginAnnotation.getElementValues()
                    .get(categoryKey)
                    .accept(new SimpleAnnotationValueVisitor9<String, Void>() {
                        @Override
                        public String visitString(final String s, final Void unused1) {
                            return s;
                        }
                    }, null);
            pluginCategories.computeIfAbsent(category, ignored -> new HashSet<>()).add(e);

            return null;
        }
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        final TypeElement[] producerAnnotations = annotations.stream()
                .filter(e -> e.getAnnotation(Producer.class) != null)
                .toArray(TypeElement[]::new);
        final var producesAnnotationVisitor = new ProducerAnnotationVisitor();
        roundEnv.getElementsAnnotatedWithAny(producerAnnotations).forEach(producesAnnotationVisitor::visit);

        final var disposesAnnotationVisitor = new DisposesAnnotationVisitor();
        roundEnv.getElementsAnnotatedWith(Disposes.class).forEach(disposesAnnotationVisitor::visit);

        final var injectAnnotationVisitor = new InjectAnnotationVisitor();
        roundEnv.getElementsAnnotatedWith(Inject.class).forEach(injectAnnotationVisitor::visit);

        final Types types = processingEnv.getTypeUtils();
        final var qualifiedAnnotationVisitor = new QualifiedAnnotationVisitor(annotationMirror -> {
            for (final TypeElement producerAnnotation : producerAnnotations) {
                if (types.isSameType(producerAnnotation.asType(), annotationMirror.getAnnotationType())) {
                    return true;
                }
            }
            return false;
        });
        final TypeElement[] qualifierAnnotations = annotations.stream()
                .filter(e -> e.getAnnotation(Qualifier.class) != null)
                .toArray(TypeElement[]::new);
        roundEnv.getElementsAnnotatedWithAny(qualifierAnnotations).forEach(qualifiedAnnotationVisitor::visit);

        final TypeElement[] pluginAnnotations = annotations.stream()
                .filter(e -> e.getSimpleName().contentEquals("Plugin"))
                .toArray(TypeElement[]::new);
        final var pluginAnnotationVisitor = new PluginAnnotationVisitor();
        roundEnv.getElementsAnnotatedWithAny(pluginAnnotations).forEach(pluginAnnotationVisitor::visit);

        final Set<ExecutableElement> producerMethods = producesAnnotationVisitor.producerMethods;
        final Set<VariableElement> producerFields = producesAnnotationVisitor.producerFields;
        final Set<ExecutableElement> disposesMethods = disposesAnnotationVisitor.disposesMethods;
        final Set<TypeElement> injectableClasses = injectAnnotationVisitor.injectableClasses;
        final Set<TypeElement> implicitInjectableClasses = qualifiedAnnotationVisitor.injectableClasses;
        final Map<String, Set<TypeElement>> pluginCategories = pluginAnnotationVisitor.pluginCategories;
        final Set<PackageElement> packageElements = new HashSet<>();

        final Elements elements = processingEnv.getElementUtils();
        final Set<CharSequence> producibleClassNames = Stream.concat(
                producerMethods.stream()
                        .map(e -> (TypeElement) e.getEnclosingElement())
                        .peek(e -> packageElements.add(elements.getPackageOf(e)))
                        .map(elements::getBinaryName),
                producerFields.stream()
                        .map(e -> (TypeElement) e.getEnclosingElement())
                        .peek(e -> packageElements.add(elements.getPackageOf(e)))
                        .map(elements::getBinaryName))
                .collect(Collectors.toSet());
        final Set<CharSequence> destructibleClassNames = disposesMethods.stream()
                .map(e -> (TypeElement) e.getEnclosingElement())
                .peek(e -> packageElements.add(elements.getPackageOf(e)))
                .map(elements::getBinaryName)
                .collect(Collectors.toSet());
        final Set<CharSequence> injectableClassNames = Stream.concat(
                injectableClasses.stream()
                        .peek(e -> packageElements.add(elements.getPackageOf(e)))
                        .map(elements::getBinaryName),
                implicitInjectableClasses.stream()
                        .peek(e -> packageElements.add(elements.getPackageOf(e)))
                        .map(elements::getBinaryName))
                .collect(Collectors.toSet());
        final Map<String, List<CharSequence>> pluginClassNames = pluginCategories.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .peek(el -> packageElements.add(elements.getPackageOf(el)))
                                .map(elements::getBinaryName)
                                .sorted(CharSequence::compare)
                                .collect(Collectors.toList())));

        String packageName = processingEnv.getOptions().get("pluginPackage");
        if (packageName == null) {
            packageName = packageElements.stream()
                    .map(PackageElement::getQualifiedName)
                    .map(CharSequence.class::cast)
                    .reduce(BeanProcessor::commonPrefix)
                    .orElseThrow()
                    .toString();
        }
        try {
            writeBeanInfoServiceFile(packageName);
            writeBeanInfoServiceClass(packageName, injectableClassNames, producibleClassNames, destructibleClassNames, pluginClassNames);
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeBeanInfoServiceFile(final String packageName) throws IOException {
        final FileObject fileObject = processingEnv.getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", BEAN_INFO_SERVICE_FILE);
        try (PrintWriter out = new PrintWriter(fileObject.openWriter())) {
            out.println(packageName + ".plugins.Log4jBeanInfo");
        }
    }

    private void writeBeanInfoServiceClass(final String packageName, final Set<CharSequence> injectableClassNames,
                                           final Set<CharSequence> producibleClassNames,
                                           final Set<CharSequence> destructibleClassNames,
                                           final Map<String, List<CharSequence>> pluginClassNames)
            throws IOException {
        final JavaFileObject sourceFile = processingEnv.getFiler()
                .createSourceFile(packageName + ".plugins.Log4jBeanInfo");
        try (final PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
            out.println("package " + packageName + ".plugins;");
            out.println();
            out.println("import java.util.List;");
            out.println("import java.util.Map;");
            out.println();
            out.println("@javax.annotation.processing.Generated(\"" + getClass().getName() + "\")");
            out.println("public class Log4jBeanInfo extends org.apache.logging.log4j.plugins.di.spi.BeanInfoService {");
            out.println();
            out.println("  private static final List<String> INJECTABLE = List.of(" + getListOfNames(injectableClassNames) + ");");
            out.println();
            out.println("  private static final List<String> PRODUCIBLE = List.of(" + getListOfNames(producibleClassNames) + ");");
            out.println();
            out.println("  private static final List<String> DESTRUCTIBLE = List.of(" + getListOfNames(destructibleClassNames) + ");");
            out.println();
            out.println("  private static final Map<String, List<String>> PLUGIN_CATEGORIES = Map.of(" + getMapOfPluginNames(pluginClassNames) + ");");
            out.println();
            out.println("  public Log4jBeanInfo() {");
            out.println("    super(INJECTABLE, PRODUCIBLE, DESTRUCTIBLE, PLUGIN_CATEGORIES);");
            out.println("  }");
            out.println();
            out.println("}");
        }
    }

    private static String getListOfNames(final Set<CharSequence> names) {
        return names.isEmpty() ? Strings.EMPTY : names.stream().sorted(CharSequence::compare).collect(
                Collectors.joining("\",\n    \"", "\n    \"", "\"\n  "));
    }

    private static String getMapOfPluginNames(final Map<String, List<CharSequence>> pluginClassNames) {
        return pluginClassNames.isEmpty() ? Strings.EMPTY : pluginClassNames.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> '"' + e.getKey() + "\", List.of(" + e.getValue().stream().collect(Collectors.joining("\",\n      \"", "\n      \"", "\"\n    ")) + ')')
                .collect(Collectors.joining(",\n    ", "\n    ", "\n  "));
    }

    private static CharSequence commonPrefix(final CharSequence str1, final CharSequence str2) {
        final int minLength = Math.min(str1.length(), str2.length());
        for (int i = 0; i < minLength; i++) {
            if (str1.charAt(i) != str2.charAt(i)) {
                if (i > 1 && str1.charAt(i - 1) == '.') {
                    return str1.subSequence(0, i - 1);
                } else {
                    return str1.subSequence(0, i);
                }
            }
        }
        return str1.subSequence(0, minLength);
    }

}
