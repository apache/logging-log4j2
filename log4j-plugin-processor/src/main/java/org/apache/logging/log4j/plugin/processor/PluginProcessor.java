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
package org.apache.logging.log4j.plugin.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.model.PluginEntry;
import org.apache.logging.log4j.util.Strings;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Annotation processor for pre-scanning Log4j 2 plugins.
 */
@SupportedAnnotationTypes({"org.apache.logging.log4j.plugins.*", "org.apache.logging.log4j.core.config.plugins.*"})
public class PluginProcessor extends AbstractProcessor {

    // TODO: this could be made more abstract to allow for compile-time and run-time plugin processing

    private static final String SERVICE_FILE_NAME =
            "META-INF/services/org.apache.logging.log4j.plugins.model.PluginService";

    public PluginProcessor() {
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        Map<String, String> options = processingEnv.getOptions();
        String packageName = options.get("pluginPackage");
        Messager messager = processingEnv.getMessager();
        messager.printMessage(Kind.NOTE, "Processing Log4j annotations");
        try {
            final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Plugin.class);
            if (elements.isEmpty()) {
                messager.printMessage(Kind.NOTE, "No elements to process");
                return false;
            }
            messager.printMessage(Kind.NOTE, "Retrieved " + elements.size() + " Plugin elements");
            List<PluginEntryMirror> list = new ArrayList<>();
            packageName = collectPlugins(packageName, elements, list);
            writeClassFile(packageName, list);
            writeServiceFile(packageName);
            messager.printMessage(Kind.NOTE, "Annotations processed");
        } catch (final Exception ex) {
            error(ex.getMessage());
        }
        return false;
    }

    private void error(final CharSequence message) {
        processingEnv.getMessager().printMessage(Kind.ERROR, message);
    }

    private String collectPlugins(String packageName, final Iterable<? extends Element> elements, List<PluginEntryMirror> list) {
        boolean calculatePackage = packageName == null;
        final Elements elementUtils = processingEnv.getElementUtils();
        final ElementVisitor<PluginEntryMirror, Plugin> pluginVisitor = new PluginElementVisitor(elementUtils);
        final ElementVisitor<Collection<PluginEntryMirror>, Plugin> pluginAliasesVisitor = new PluginAliasesElementVisitor(
                elementUtils);
        for (final Element element : elements) {
            final Plugin plugin = element.getAnnotation(Plugin.class);
            if (plugin == null) {
                continue;
            }
            final PluginEntryMirror entry = element.accept(pluginVisitor, plugin);
            list.add(entry);
            if (calculatePackage) {
                packageName = calculatePackage(elementUtils, element, packageName);
            }
            list.addAll(element.accept(pluginAliasesVisitor, plugin));
        }
        return packageName;
    }

    private String calculatePackage(Elements elements, Element element, String packageName) {
        Name name = elements.getPackageOf(element).getQualifiedName();
        if (name == null) {
            return null;
        }
        String pkgName = name.toString();
        if (packageName == null) {
            return pkgName;
        }
        if (pkgName.length() == packageName.length()) {
            return packageName;
        }
        if (pkgName.length() < packageName.length() && packageName.startsWith(pkgName)) {
            return pkgName;
        }

        return commonPrefix(pkgName, packageName);
    }

    private void writeServiceFile(String pkgName) throws IOException {
        final FileObject fileObject = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, Strings.EMPTY,
                SERVICE_FILE_NAME);
        try (final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fileObject.openOutputStream(), UTF_8)))) {
            writer.println(createFqcn(pkgName));
        }
    }

    private void writeClassFile(String pkg, List<PluginEntryMirror> list) {
        String fqcn = createFqcn(pkg);
        try (final PrintWriter writer = createSourceFile(fqcn)) {
            writer.println("package " + pkg + ".plugins;");
            writer.println("");
            writer.println("import org.apache.logging.log4j.plugins.model.PluginEntry;");
            writer.println("import org.apache.logging.log4j.plugins.model.PluginService;");
            writer.println("");
            writer.println("public class Log4jPlugins extends PluginService {");
            writer.println("");
            writer.println("  private static final PluginEntry[] ENTRIES = new PluginEntry[] {");
            int max = list.size() - 1;
            for (int i = 0; i < list.size(); ++i) {
                PluginEntryMirror mirror = list.get(i);
                final PluginEntry entry = mirror.entry;
                writer.println("    PluginEntry.builder()");
                writer.println(String.format("      .setKey(\"%s\")", entry.getKey()));
                writer.println(String.format("      .setClassName(\"%s\")", entry.getClassName()));
                writer.println(String.format("      .setName(\"%s\")", entry.getName()));
                writer.println(String.format("      .setNamespace(\"%s\")", entry.getNamespace()));
                final String elementType = entry.getElementType();
                if (Strings.isNotEmpty(elementType)) {
                    writer.println(String.format("      .setElementType(\"%s\")", elementType));
                }
                if (entry.isPrintable()) {
                    writer.println("      .setPrintable(true)");
                }
                if (entry.isDeferChildren()) {
                    writer.println("      .setDeferChildren(true)");
                }
                writer.println("      .get()" + (i < max ? "," : Strings.EMPTY));
            }
            writer.println("    };");
            writer.println("    @Override");
            writer.println("    public PluginEntry[] getEntries() { return ENTRIES; }");
            writer.println("}");
        }
    }

    private PrintWriter createSourceFile(String fqcn) {
        try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(fqcn);
            return new PrintWriter(sourceFile.openWriter());
        } catch (IOException e) {
            throw new LoggingException("Unable to create Plugin Service Class " + fqcn, e);
        }
    }

    private String createFqcn(String packageName) {
        return packageName + ".plugins.Log4jPlugins";
    }

    private static class PluginEntryMirror {
        private final TypeElement element;
        private final PluginEntry entry;

        private PluginEntryMirror(final TypeElement element, final PluginEntry entry) {
            this.element = element;
            this.entry = entry;
        }
    }

    private static String getNamespace(final TypeElement e) {
        return Optional.ofNullable(e.getAnnotation(Namespace.class)).map(Namespace::value).orElseGet(
                () -> e.getAnnotationMirrors().stream().flatMap(
                                annotationMirror -> annotationMirror.getAnnotationType().asElement().getAnnotationMirrors().stream())
                        .filter(annotationMirror -> annotationMirror.getAnnotationType().asElement().getSimpleName()
                                .contentEquals(Namespace.class.getSimpleName())).flatMap(
                                annotationMirror -> annotationMirror.getElementValues().values().stream()
                                        .map(AnnotationValue::getValue).map(Objects::toString)).findFirst()
                        .orElse(Plugin.EMPTY));
    }

    /**
     * ElementVisitor to scan the Plugin annotation.
     */
    private static class PluginElementVisitor extends SimpleElementVisitor8<PluginEntryMirror, Plugin> {

        private final Elements elements;

        private PluginElementVisitor(final Elements elements) {
            this.elements = elements;
        }

        @Override
        public PluginEntryMirror visitType(final TypeElement e, final Plugin plugin) {
            Objects.requireNonNull(plugin, "Plugin annotation is null.");
            String name = plugin.value();
            if (name.isEmpty()) {
                name = e.getSimpleName().toString();
            }
            final PluginEntry.Builder builder = PluginEntry.builder()
                    .setKey(name.toLowerCase(Locale.ROOT))
                    .setName(name)
                    .setClassName(elements.getBinaryName(e).toString());
            Configurable configurable = e.getAnnotation(Configurable.class);
            if (configurable != null) {
                builder.setNamespace(Node.CORE_NAMESPACE)
                        .setElementType(configurable.elementType().isEmpty() ? name : configurable.elementType())
                        .setDeferChildren(configurable.deferChildren())
                        .setPrintable(configurable.printObject());
            } else {
                builder.setNamespace(getNamespace(e));
            }
            return new PluginEntryMirror(e, builder.get());
        }
    }

    private String commonPrefix(String str1, String str2) {
        int minLength = Math.min(str1.length(), str2.length());
        for (int i = 0; i < minLength; i++) {
            if (str1.charAt(i) != str2.charAt(i)) {
                if (i > 1 && str1.charAt(i-1) == '.') {
                    return str1.substring(0, i-1);
                } else {
                    return str1.substring(0, i);
                }
            }
        }
        return str1.substring(0, minLength);
    }

    /**
     * ElementVisitor to scan the PluginAliases annotation.
     */
    private static class PluginAliasesElementVisitor extends SimpleElementVisitor8<Collection<PluginEntryMirror>, Plugin> {

        private final Elements elements;

        private PluginAliasesElementVisitor(final Elements elements) {
            super(Collections.emptyList());
            this.elements = elements;
        }

        @Override
        public Collection<PluginEntryMirror> visitType(final TypeElement e, final Plugin plugin) {
            final PluginAliases aliases = e.getAnnotation(PluginAliases.class);
            if (aliases == null) {
                return DEFAULT_VALUE;
            }
            String name = plugin.value();
            if (name.isEmpty()) {
                name = e.getSimpleName().toString();
            }
            final PluginEntry.Builder builder = PluginEntry.builder()
                    .setName(name)
                    .setClassName(elements.getBinaryName(e).toString());
            Configurable configurable = e.getAnnotation(Configurable.class);
            if (configurable != null) {
                builder.setNamespace(Node.CORE_NAMESPACE)
                        .setElementType(configurable.elementType().isEmpty() ? name : configurable.elementType())
                        .setDeferChildren(configurable.deferChildren())
                        .setPrintable(configurable.printObject());
            } else {
                builder.setNamespace(getNamespace(e));
            }
            final Collection<PluginEntryMirror> entries = new ArrayList<>(aliases.value().length);
            for (final String alias : aliases.value()) {
                final PluginEntry entry = builder.setKey(alias.toLowerCase(Locale.ROOT)).get();
                entries.add(new PluginEntryMirror(e, entry));
            }
            return entries;
        }
    }
}
