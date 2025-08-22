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
package org.apache.logging.log4j.plugin.processor;

import static java.nio.charset.StandardCharsets.UTF_8;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.model.PluginEntry;
import org.apache.logging.log4j.plugins.model.PluginIndex;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.NullMarked;

/**
 * Annotation processor to generate a {@link org.apache.logging.log4j.plugins.model.PluginService} implementation.
 * <p>
 *     This generates a {@link org.apache.logging.log4j.plugins.model.PluginService} implementation with a list of
 *     {@link PluginEntry} instances.
 *     The fully qualified class name of the generated service is:
 * </p>
 * <pre>
 *     {@code <log4j.plugin.package>.plugins.Log4jPlugins}
 * </pre>
 * <p>
 *     where {@code <log4j.plugin.package>} is the effective value of the {@link #PLUGIN_PACKAGE} option.
 * </p>
 */
@NullMarked
@SupportedAnnotationTypes("org.apache.logging.log4j.plugins.Plugin")
@ServiceProvider(value = Processor.class, resolution = Resolution.OPTIONAL)
public class PluginProcessor extends AbstractProcessor {

    /**
     * Option name to enable or disable the generation of {@link aQute.bnd.annotation.spi.ServiceConsumer} annotations.
     * <p>
     *     The default behavior depends on the presence of {@code biz.aQute.bnd.annotation} on the classpath.
     * </p>
     */
    public static final String ENABLE_BND_ANNOTATIONS = "log4j.plugin.enableBndAnnotations";

    /**
     * Option name to determine the package containing the generated {@link org.apache.logging.log4j.plugins.model.PluginService}
     * <p>
     *     If absent, the value of this option is the common prefix of all Log4j Plugin classes.
     * </p>
     */
    public static final String PLUGIN_PACKAGE = "log4j.plugin.package";

    private static final String SERVICE_FILE_NAME =
            "META-INF/services/org.apache.logging.log4j.plugins.model.PluginService";

    private boolean enableBndAnnotations;
    private CharSequence packageName = "";
    private final PluginIndex pluginIndex = new PluginIndex();
    private final Set<TypeElement> processedElements = new HashSet<>();

    public PluginProcessor() {}

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(ENABLE_BND_ANNOTATIONS, PLUGIN_PACKAGE);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        handleOptions();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        // Process the elements for this round
        if (!annotations.isEmpty()) {
            processPluginAnnotatedClasses(ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Plugin.class)));
        }
        // Write the generated code
        if (roundEnv.processingOver() && !pluginIndex.isEmpty()) {
            try {
                final Messager messager = processingEnv.getMessager();
                messager.printMessage(Kind.NOTE, "Writing Log4j plugin metadata using base package " + packageName);
                writeClassFile();
                writeServiceFile();
                messager.printMessage(Kind.NOTE, "Log4j annotations processed");
            } catch (final Exception e) {
                handleUnexpectedError(e);
            }
        }
        // Do not claim the annotations to allow other annotation processors to run
        return false;
    }

    private void processPluginAnnotatedClasses(Set<TypeElement> pluginClasses) {
        final boolean calculatePackageName = packageName.isEmpty();
        final Elements elements = processingEnv.getElementUtils();
        final Messager messager = processingEnv.getMessager();
        for (var pluginClass : pluginClasses) {
            final String name = getPluginName(pluginClass);
            final String namespace = getNamespace(pluginClass);
            final String className = elements.getBinaryName(pluginClass).toString();
            var builder =
                    PluginEntry.builder().setName(name).setNamespace(namespace).setClassName(className);
            processConfigurableAnnotation(pluginClass, builder);
            var entry = builder.get();
            messager.printMessage(Kind.NOTE, "Parsed Log4j plugin " + entry, pluginClass);
            if (!pluginIndex.add(entry)) {
                messager.printMessage(Kind.WARNING, "Duplicate Log4j plugin parsed " + entry, pluginClass);
            }
            pluginIndex.addAll(createPluginAliases(pluginClass, builder));
            if (calculatePackageName) {
                packageName = calculatePackageName(elements, pluginClass, packageName);
            }
            processedElements.add(pluginClass);
        }
    }

    private static void processConfigurableAnnotation(TypeElement pluginClass, PluginEntry.Builder builder) {
        var configurable = pluginClass.getAnnotation(Configurable.class);
        if (configurable != null) {
            var elementType = configurable.elementType();
            builder.setElementType(elementType.isEmpty() ? builder.getName() : elementType)
                    .setDeferChildren(configurable.deferChildren())
                    .setPrintable(configurable.printObject());
        }
    }

    private static List<PluginEntry> createPluginAliases(TypeElement pluginClass, PluginEntry.Builder builder) {
        return Optional.ofNullable(pluginClass.getAnnotation(PluginAliases.class)).map(PluginAliases::value).stream()
                .flatMap(Arrays::stream)
                .map(alias -> alias.toLowerCase(Locale.ROOT))
                .map(key -> builder.setKey(key).get())
                .toList();
    }

    private void handleUnexpectedError(final Exception e) {
        var writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        processingEnv
                .getMessager()
                .printMessage(Kind.ERROR, "Unexpected error processing Log4j annotations: " + writer);
    }

    private static CharSequence calculatePackageName(
            Elements elements, TypeElement typeElement, CharSequence packageName) {
        var qualifiedName = elements.getPackageOf(typeElement).getQualifiedName();
        if (qualifiedName.isEmpty()) {
            return packageName;
        }
        if (packageName.isEmpty()) {
            return qualifiedName;
        }
        int packageLength = packageName.length();
        int qualifiedLength = qualifiedName.length();
        if (packageLength == qualifiedLength) {
            return packageName;
        }
        if (qualifiedLength < packageLength
                && qualifiedName.contentEquals(packageName.subSequence(0, qualifiedLength))) {
            return qualifiedName;
        }
        return commonPrefix(qualifiedName, packageName);
    }

    private void writeServiceFile() throws IOException {
        final FileObject fileObject = processingEnv
                .getFiler()
                .createResource(
                        StandardLocation.CLASS_OUTPUT,
                        Strings.EMPTY,
                        SERVICE_FILE_NAME,
                        processedElements.toArray(TypeElement[]::new));
        try (final PrintWriter writer =
                new PrintWriter(new BufferedWriter(new OutputStreamWriter(fileObject.openOutputStream(), UTF_8)))) {
            writer.println("# Generated by " + PluginProcessor.class.getName());
            writer.println(createFqcn(packageName));
        }
    }

    private void writeClassFile() {
        final String fqcn = createFqcn(packageName);
        try (final PrintWriter writer = createSourceFile(fqcn)) {
            writer.println("package " + packageName + ".plugins;");
            writer.println("");
            if (enableBndAnnotations) {
                writer.println("import aQute.bnd.annotation.Resolution;");
                writer.println("import aQute.bnd.annotation.spi.ServiceProvider;");
            }
            writer.println("import org.apache.logging.log4j.plugins.model.PluginEntry;");
            writer.println("import org.apache.logging.log4j.plugins.model.PluginService;");
            writer.println("");
            if (enableBndAnnotations) {
                writer.println("@ServiceProvider(value = PluginService.class, resolution = Resolution.OPTIONAL)");
            }
            writer.println("public class Log4jPlugins extends PluginService {");
            writer.println("");
            writer.println("  private static final PluginEntry[] ENTRIES = new PluginEntry[] {");
            final int max = pluginIndex.size() - 1;
            int current = 0;
            for (final PluginEntry entry : pluginIndex) {
                writer.println("    PluginEntry.builder()");
                writer.println(String.format("      .setKey(\"%s\")", entry.key()));
                writer.println(String.format("      .setClassName(\"%s\")", entry.className()));
                writer.println(String.format("      .setName(\"%s\")", entry.name()));
                writer.println(String.format("      .setNamespace(\"%s\")", entry.namespace()));
                final String elementType = entry.elementType();
                if (Strings.isNotEmpty(elementType)) {
                    writer.println(String.format("      .setElementType(\"%s\")", elementType));
                }
                if (entry.printable()) {
                    writer.println("      .setPrintable(true)");
                }
                if (entry.deferChildren()) {
                    writer.println("      .setDeferChildren(true)");
                }
                writer.println("      .get()" + (current < max ? "," : Strings.EMPTY));
                current++;
            }
            writer.println("    };");
            writer.println("    @Override");
            writer.println("    public PluginEntry[] getEntries() { return ENTRIES; }");
            writer.println("}");
        }
    }

    private PrintWriter createSourceFile(final String fqcn) {
        try {
            final JavaFileObject sourceFile =
                    processingEnv.getFiler().createSourceFile(fqcn, processedElements.toArray(TypeElement[]::new));
            return new PrintWriter(sourceFile.openWriter());
        } catch (IOException e) {
            throw new LoggingException("Unable to create Plugin Service Class " + fqcn, e);
        }
    }

    private String createFqcn(CharSequence packageName) {
        return packageName + ".plugins.Log4jPlugins";
    }

    private static String getPluginName(TypeElement pluginClass) {
        return Optional.ofNullable(pluginClass.getAnnotation(Plugin.class))
                .map(Plugin::value)
                .filter(s -> !s.isEmpty())
                .orElseGet(() -> pluginClass.getSimpleName().toString());
    }

    private static String getNamespace(final TypeElement e) {
        return Optional.ofNullable(e.getAnnotation(Namespace.class))
                .map(Namespace::value)
                .orElseGet(() -> e.getAnnotationMirrors().stream()
                        .flatMap(annotationMirror ->
                                annotationMirror.getAnnotationType().asElement().getAnnotationMirrors().stream())
                        .filter(annotationMirror -> annotationMirror
                                .getAnnotationType()
                                .asElement()
                                .getSimpleName()
                                .contentEquals(Namespace.class.getSimpleName()))
                        .flatMap(annotationMirror -> annotationMirror.getElementValues().values().stream()
                                .map(AnnotationValue::getValue)
                                .map(Objects::toString))
                        .findFirst()
                        .orElse(Plugin.EMPTY));
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

    private boolean isServiceConsumerClassPresent() {
        // Looks for the presence of the annotation on the classpath, not the annotation processor path.
        return processingEnv.getElementUtils().getTypeElement("aQute.bnd.annotation.spi.ServiceConsumer") != null;
    }

    private void handleOptions() {
        var options = processingEnv.getOptions();
        packageName = options.getOrDefault(PLUGIN_PACKAGE, "");
        String enableBndAnnotationsOption = options.get(ENABLE_BND_ANNOTATIONS);
        if (enableBndAnnotationsOption != null) {
            this.enableBndAnnotations = !"false".equals(enableBndAnnotationsOption);
        } else {
            this.enableBndAnnotations = isServiceConsumerClassPresent();
        }
    }
}
