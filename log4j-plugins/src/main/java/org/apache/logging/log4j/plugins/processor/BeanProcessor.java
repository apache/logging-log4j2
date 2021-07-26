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

import org.apache.logging.log4j.plugins.di.DependentScoped;
import org.apache.logging.log4j.plugins.di.Disposes;
import org.apache.logging.log4j.plugins.di.Inject;
import org.apache.logging.log4j.plugins.di.Producer;
import org.apache.logging.log4j.plugins.di.Qualifier;
import org.apache.logging.log4j.plugins.di.ScopeType;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementKindVisitor9;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor9;
import javax.lang.model.util.Types;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: migrate to separate maven module between log4j-plugins and log4j-core
@SupportedAnnotationTypes({"org.apache.logging.log4j.plugins.*", "org.apache.logging.log4j.core.config.plugins.*"})
@SupportedOptions({"pluginPackage", "pluginClassName"})
public class BeanProcessor extends AbstractProcessor {
    public static final String PLUGIN_MODULE_SERVICE_FILE = "META-INF/services/org.apache.logging.log4j.plugins.di.model.PluginModule";

    public BeanProcessor() {
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private static class ProducerAnnotationVisitor extends ElementKindVisitor9<Void, Void> {
        private final List<ProducerMethodMirror> producerMethods = new ArrayList<>();
        private final List<ProducerFieldMirror> producerFields = new ArrayList<>();

        @Override
        public Void visitVariableAsField(final VariableElement e, final Void unused) {
            producerFields.add(new ProducerFieldMirror(e));
            return null;
        }

        @Override
        public Void visitExecutableAsMethod(final ExecutableElement e, final Void unused) {
            producerMethods.add(new ProducerMethodMirror(e));
            return null;
        }
    }

    private static class DisposesAnnotationVisitor extends ElementKindVisitor9<Void, Void> {
        private final List<DisposesMirror> disposesParameters = new ArrayList<>();

        @Override
        public Void visitVariableAsParameter(final VariableElement e, final Void unused) {
            disposesParameters.add(new DisposesMirror(e));
            return null;
        }
    }

    private static class InjectAnnotationVisitor extends ElementKindVisitor9<Void, Void> {
        private final List<InjectionTargetMirror> injectableClasses = new ArrayList<>();

        @Override
        public Void visitVariableAsField(final VariableElement e, final Void unused) {
            injectableClasses.add(new InjectionTargetMirror(((TypeElement) e.getEnclosingElement())));
            return null;
        }

        @Override
        public Void visitExecutableAsConstructor(final ExecutableElement e, final Void unused) {
            injectableClasses.add(new InjectionTargetMirror((TypeElement) e.getEnclosingElement()));
            return null;
        }

        @Override
        public Void visitExecutableAsMethod(final ExecutableElement e, final Void unused) {
            injectableClasses.add(new InjectionTargetMirror((TypeElement) e.getEnclosingElement()));
            return null;
        }
    }

    private static class QualifiedAnnotationVisitor extends ElementKindVisitor9<Void, Void> {
        private final Predicate<AnnotationMirror> isProducerAnnotation;
        private final List<InjectionTargetMirror> injectableClasses = new ArrayList<>();

        private QualifiedAnnotationVisitor(final Predicate<AnnotationMirror> isProducerAnnotation) {
            this.isProducerAnnotation = isProducerAnnotation;
        }

        @Override
        public Void visitVariableAsField(final VariableElement e, final Void unused) {
            if (e.getAnnotationMirrors().stream().noneMatch(isProducerAnnotation)) {
                injectableClasses.add(new InjectionTargetMirror((TypeElement) e.getEnclosingElement()));
            }
            return null;
        }

        @Override
        public Void visitVariableAsParameter(final VariableElement e, final Void unused) {
            final Element enclosingExecutable = e.getEnclosingElement();
            final TypeElement typeElement = (TypeElement) enclosingExecutable.getEnclosingElement();
            if (enclosingExecutable.getKind() == ElementKind.CONSTRUCTOR ||
                    enclosingExecutable.getAnnotationMirrors().stream().noneMatch(isProducerAnnotation)) {
                injectableClasses.add(new InjectionTargetMirror(typeElement));
            }
            return null;
        }
    }

    private static class PluginAnnotationVisitor extends ElementKindVisitor9<Void, Void> {
        private final List<GenericPluginMirror> plugins = new ArrayList<>();

        @Override
        public Void visitTypeAsClass(final TypeElement e, final Void unused) {
            plugins.add(new GenericPluginMirror(e));
            return null;
        }
    }

    private static class ScopeTypeVisitor extends ElementKindVisitor9<TypeElement, Types> {
        protected ScopeTypeVisitor(final TypeElement defaultValue) {
            super(defaultValue);
        }

        @Override
        public TypeElement visitType(final TypeElement e, final Types types) {
            for (final AnnotationMirror annotationMirror : e.getAnnotationMirrors()) {
                final DeclaredType annotationType = annotationMirror.getAnnotationType();
                if (annotationType.getAnnotation(ScopeType.class) != null) {
                    return (TypeElement) annotationType.asElement();
                }
            }
            return super.visitType(e, types);
        }

        @Override
        public TypeElement visitVariableAsField(final VariableElement e, final Types types) {
            return Stream.concat(e.getAnnotationMirrors().stream(), e.asType().getAnnotationMirrors().stream())
                    .map(AnnotationMirror::getAnnotationType)
                    .filter(type -> type.getAnnotation(ScopeType.class) != null)
                    .findFirst()
                    .map(type -> (TypeElement) type.asElement())
                    .orElse(super.DEFAULT_VALUE);
        }

        @Override
        public TypeElement visitExecutableAsMethod(final ExecutableElement e, final Types types) {
            return Stream.concat(e.getAnnotationMirrors().stream(), e.getReturnType().getAnnotationMirrors().stream())
                    .map(AnnotationMirror::getAnnotationType)
                    .filter(type -> type.getAnnotation(ScopeType.class) != null)
                    .findFirst()
                    .map(type -> (TypeElement) type.asElement())
                    .orElse(super.DEFAULT_VALUE);
        }
    }

    interface PluginSourceMirror<E extends Element> {
        E getElement();

        TypeElement getDeclaringElement();

        TypeMirror getType();
    }

    static class ProducerMethodMirror implements PluginSourceMirror<ExecutableElement> {
        private final ExecutableElement element;

        ProducerMethodMirror(final ExecutableElement element) {
            this.element = element;
        }

        @Override
        public ExecutableElement getElement() {
            return element;
        }

        @Override
        public TypeElement getDeclaringElement() {
            return (TypeElement) element.getEnclosingElement();
        }

        @Override
        public TypeMirror getType() {
            return element.getReturnType();
        }
    }

    static class ProducerFieldMirror implements PluginSourceMirror<VariableElement> {
        private final VariableElement element;

        ProducerFieldMirror(final VariableElement element) {
            this.element = element;
        }

        @Override
        public VariableElement getElement() {
            return element;
        }

        @Override
        public TypeElement getDeclaringElement() {
            return (TypeElement) element.getEnclosingElement();
        }

        @Override
        public TypeMirror getType() {
            return element.asType();
        }
    }

    static class InjectionTargetMirror implements PluginSourceMirror<TypeElement> {
        private final TypeElement element;

        InjectionTargetMirror(final TypeElement element) {
            this.element = element;
        }

        @Override
        public TypeElement getElement() {
            return element;
        }

        @Override
        public TypeElement getDeclaringElement() {
            return element;
        }

        @Override
        public TypeMirror getType() {
            return element.asType();
        }
    }

    static class DisposesMirror implements PluginSourceMirror<VariableElement> {
        private final VariableElement element;

        DisposesMirror(final VariableElement element) {
            this.element = element;
        }

        @Override
        public VariableElement getElement() {
            return element;
        }

        @Override
        public TypeElement getDeclaringElement() {
            return (TypeElement) element.getEnclosingElement().getEnclosingElement();
        }

        @Override
        public TypeMirror getType() {
            return element.asType();
        }
    }

    static class GenericPluginMirror implements PluginSourceMirror<TypeElement> {
        private final TypeElement element;

        GenericPluginMirror(final TypeElement element) {
            this.element = element;
        }

        @Override
        public TypeElement getElement() {
            return element;
        }

        @Override
        public TypeElement getDeclaringElement() {
            return element;
        }

        @Override
        public TypeMirror getType() {
            return element.asType();
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

        final Set<PackageElement> packageElements = new HashSet<>();
        final Set<TypeElement> declaringTypes = new HashSet<>();

        final Elements elements = processingEnv.getElementUtils();
        final List<PluginSourceMirror<?>> mirrors = new ArrayList<>(producesAnnotationVisitor.producerMethods);
        mirrors.addAll(producesAnnotationVisitor.producerFields);
        mirrors.addAll(injectAnnotationVisitor.injectableClasses);
        mirrors.addAll(disposesAnnotationVisitor.disposesParameters);
        mirrors.forEach(mirror -> {
            declaringTypes.add(mirror.getDeclaringElement());
            packageElements.add(elements.getPackageOf(mirror.getDeclaringElement()));
        });

        qualifiedAnnotationVisitor.injectableClasses.stream()
                .filter(mirror -> !declaringTypes.contains(mirror.getDeclaringElement()))
                .forEach(mirror -> {
                    mirrors.add(mirror);
                    declaringTypes.add(mirror.getDeclaringElement());
                    packageElements.add(elements.getPackageOf(mirror.getDeclaringElement()));
                });

        pluginAnnotationVisitor.plugins.stream()
                .filter(mirror -> !declaringTypes.contains(mirror.getDeclaringElement()))
                .forEach(mirror -> {
                    mirrors.add(mirror);
                    declaringTypes.add(mirror.getDeclaringElement());
                    packageElements.add(elements.getPackageOf(mirror.getDeclaringElement()));
                });

        String packageName = processingEnv.getOptions().get("pluginPackage");
        if (packageName == null) {
            packageName = packageElements.stream()
                    .map(PackageElement::getQualifiedName)
                    .map(CharSequence.class::cast)
                    .reduce(BeanProcessor::commonPrefix)
                    .orElseThrow()
                    .toString();
        }
        String className = processingEnv.getOptions().getOrDefault("pluginClassName", "Log4jModule");
        try {
            writePluginModule(packageName, className, mirrors);
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writePluginModule(final CharSequence packageName, final CharSequence className,
                                   final List<PluginSourceMirror<?>> mirrors) throws IOException {
        try (final PrintWriter out = new PrintWriter(processingEnv.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT, "", PLUGIN_MODULE_SERVICE_FILE).openWriter())) {
            out.println(packageName + ".plugins." + className);
        }
        try (final PrintWriter out = new PrintWriter(processingEnv.getFiler().createSourceFile(
                packageName + ".plugins." + className).openWriter())) {
            out.println("package " + packageName + ".plugins;");
            out.println();
            out.println("import org.apache.logging.log4j.plugins.di.model.*;");
            out.println();
            out.println("import java.util.List;");
            out.println("import java.util.Set;");
            out.println();
            out.println("@javax.annotation.processing.Generated(\"" + getClass().getName() + "\")");
            out.println("public class " + className + " extends PluginModule {");
            out.println();
            out.println("  private static final List<PluginSource> PLUGINS = List.of(" + javaListOfPlugins(mirrors) + ");");
            out.println();
            out.println("  public " + className + "() {");
            out.println("    super(PLUGINS);");
            out.println("  }");
            out.println();
            out.println("}");
        }
    }

    private String javaListOfPlugins(final List<PluginSourceMirror<?>> mirrors) {
        final Elements elements = processingEnv.getElementUtils();
        final Types types = processingEnv.getTypeUtils();
        final var scopeTypeVisitor = new ScopeTypeVisitor(elements.getTypeElement(DependentScoped.class.getCanonicalName()));
        return mirrors.stream()
                .sorted(Comparator.<PluginSourceMirror<?>, String>comparing(m -> m.getClass().getName())
                        .thenComparing(m -> elements.getBinaryName(m.getDeclaringElement()), CharSequence::compare))
                .map(mirror -> {
                    final String declaringClassName = '"' + elements.getBinaryName(mirror.getDeclaringElement()).toString() + '"';
                    final String setOfImplementedInterfaces = javaSetOfImplementedInterfaces(mirror.getType());
                    final String scopeTypeClassReference = mirror.getElement().accept(scopeTypeVisitor, types).getQualifiedName() + ".class";
                    if (mirror instanceof ProducerMethodMirror) {
                        return "new ProducerMethod(" + declaringClassName + ", \"" +
                                mirror.getType().toString() + "\", \"" +
                                mirror.getElement().getSimpleName() + "\", " +
                                setOfImplementedInterfaces + ", " +
                                scopeTypeClassReference + ")";
                    } else if (mirror instanceof ProducerFieldMirror) {
                        return "new ProducerField(" + declaringClassName + ", \"" +
                                mirror.getElement().getSimpleName() + "\", " +
                                setOfImplementedInterfaces + ", " +
                                scopeTypeClassReference + ")";
                    } else if (mirror instanceof InjectionTargetMirror) {
                        return "new InjectionTarget(" + declaringClassName + ", " +
                                setOfImplementedInterfaces + ", " +
                                scopeTypeClassReference + ")";
                    } else if (mirror instanceof DisposesMirror) {
                        return "new DisposesMethod(" + declaringClassName + ", \"" +
                                elements.getBinaryName((TypeElement) types.asElement(mirror.getElement().asType())) + "\")";
                    } else if (mirror instanceof GenericPluginMirror) {
                        return "new GenericPlugin(" + declaringClassName + ", " + setOfImplementedInterfaces + ")";
                    } else {
                        throw new UnsupportedOperationException(mirror.getClass().getName());
                    }
                })
                .collect(Collectors.joining(",\n", "\n", "\n"));
    }

    private String javaSetOfImplementedInterfaces(final TypeMirror base) {
        final Set<Name> implementedInterfaces = getImplementedInterfaces(base);
        return implementedInterfaces.isEmpty() ? "Set.of()" : "Set.of(" +
                implementedInterfaces.stream().map(name -> name + ".class").collect(Collectors.joining(", ")) +
                ")";
    }

    private Set<Name> getImplementedInterfaces(final TypeMirror base) {
        final Set<Name> implementedInterfaces = new LinkedHashSet<>();
        final Types types = processingEnv.getTypeUtils();
        base.accept(new SimpleTypeVisitor9<Void, Void>() {
            @Override
            public Void visitDeclared(final DeclaredType t, final Void unused) {
                for (final TypeMirror directSupertype : types.directSupertypes(t)) {
                    directSupertype.accept(this, null);
                }
                t.asElement().accept(new ElementKindVisitor9<Void, Void>() {
                    @Override
                    public Void visitTypeAsInterface(final TypeElement e, final Void unused) {
                        if (e.getModifiers().contains(Modifier.PUBLIC)) {
                            implementedInterfaces.add(e.getQualifiedName());
                        }
                        return null;
                    }
                }, null);
                return null;
            }
        }, null);
        return implementedInterfaces;
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
