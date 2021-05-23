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

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor7;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
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
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Annotation processor for pre-scanning Log4j 2 plugins.
 */
@SupportedAnnotationTypes({"org.apache.logging.log4j.plugins.*", "org.apache.logging.log4j.core.config.plugins.*"})
public class PluginProcessor extends AbstractProcessor {

    // TODO: this could be made more abstract to allow for compile-time and run-time plugin processing

    /**
     * The location of the plugin cache data file. This file is written to by this processor, and read from by
     * {@link org.apache.logging.log4j.plugins.util.PluginManager}.
     */
    public static final String PLUGIN_CACHE_FILE =
            "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat";
    private static final String SERVICE_FILE_NAME =
            "META-INF/services/org.apache.logging.log4j.plugins.processor.PluginService";

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
            List<PluginEntry> list = new ArrayList<>();
            packageName = collectPlugins(packageName, elements, list);
            writeClassFile(packageName, list);
            writeServiceFile(packageName);
            messager.printMessage(Kind.NOTE, "Annotations processed");
            return true;
        } catch (final Exception ex) {
            ex.printStackTrace();
            error(ex.getMessage());
            return false;
        }
    }

    private void error(final CharSequence message) {
        processingEnv.getMessager().printMessage(Kind.ERROR, message);
    }

    private String collectPlugins(String packageName, final Iterable<? extends Element> elements, List<PluginEntry> list) {
        boolean calculatePackage = packageName == null;
        final Elements elementUtils = processingEnv.getElementUtils();
        final ElementVisitor<PluginEntry, Plugin> pluginVisitor = new PluginElementVisitor(elementUtils);
        final ElementVisitor<Collection<PluginEntry>, Plugin> pluginAliasesVisitor = new PluginAliasesElementVisitor(
                elementUtils);
        for (final Element element : elements) {
            final Plugin plugin = element.getAnnotation(Plugin.class);
            if (plugin == null) {
                continue;
            }
            final PluginEntry entry = element.accept(pluginVisitor, plugin);
            list.add(entry);
            if (calculatePackage) {
                packageName = calculatePackage(elementUtils, element, packageName);
            }
            final Collection<PluginEntry> entries = element.accept(pluginAliasesVisitor, plugin);
            for (final PluginEntry pluginEntry : entries) {
                list.add(pluginEntry);
            }
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

    private void writeClassFile(String pkg, List<PluginEntry> list) {
        String fqcn = createFqcn(pkg);
        try (final PrintWriter writer = createSourceFile(fqcn)) {
            writer.println("package " + pkg + ".plugins;");
            writer.println("");
            writer.println("import org.apache.logging.log4j.plugins.processor.PluginEntry;");
            writer.println("import org.apache.logging.log4j.plugins.processor.PluginService;");
            writer.println("");
            writer.println("public class Log4jPlugins extends PluginService {");
            writer.println("");
            writer.println("    private static PluginEntry[] entries = new PluginEntry[] {");
            StringBuilder sb = new StringBuilder();
            int max = list.size() - 1;
            for (int i = 0; i < list.size(); ++i) {
                PluginEntry entry = list.get(i);
                sb.append("        ").append("new PluginEntry(\"");
                sb.append(entry.getKey()).append("\", \"");
                sb.append(entry.getClassName()).append("\", \"");
                sb.append(entry.getName()).append("\", ");
                sb.append(entry.isPrintable()).append(", ");
                sb.append(entry.isDefer()).append(", \"");
                sb.append(entry.getCategory()).append("\")");
                if (i < max) {
                    sb.append(",");
                }
                writer.println(sb.toString());
                sb.setLength(0);
            }
            writer.println("    };");
            writer.println("    @Override");
            writer.println("    public PluginEntry[] getEntries() { return entries;}");
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

    /**
     * ElementVisitor to scan the Plugin annotation.
     */
    private static class PluginElementVisitor extends SimpleElementVisitor7<PluginEntry, Plugin> {

        private final Elements elements;

        private PluginElementVisitor(final Elements elements) {
            this.elements = elements;
        }

        @Override
        public PluginEntry visitType(final TypeElement e, final Plugin plugin) {
            Objects.requireNonNull(plugin, "Plugin annotation is null.");
            final PluginEntry entry = new PluginEntry();
            entry.setKey(plugin.name().toLowerCase(Locale.US));
            entry.setClassName(elements.getBinaryName(e).toString());
            entry.setName(Plugin.EMPTY.equals(plugin.elementType()) ? plugin.name() : plugin.elementType());
            entry.setPrintable(plugin.printObject());
            entry.setDefer(plugin.deferChildren());
            entry.setCategory(plugin.category());
            return entry;
        }
    }

    private String commonPrefix(String str1, String str2) {
        int minLength = str1.length() < str2.length() ? str1.length() : str2.length();
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
    private static class PluginAliasesElementVisitor extends SimpleElementVisitor7<Collection<PluginEntry>, Plugin> {

        private final Elements elements;

        private PluginAliasesElementVisitor(final Elements elements) {
            super(Collections.<PluginEntry> emptyList());
            this.elements = elements;
        }

        @Override
        public Collection<PluginEntry> visitType(final TypeElement e, final Plugin plugin) {
            final PluginAliases aliases = e.getAnnotation(PluginAliases.class);
            if (aliases == null) {
                return DEFAULT_VALUE;
            }
            final Collection<PluginEntry> entries = new ArrayList<>(aliases.value().length);
            for (final String alias : aliases.value()) {
                final PluginEntry entry = new PluginEntry();
                entry.setKey(alias.toLowerCase(Locale.US));
                entry.setClassName(elements.getBinaryName(e).toString());
                entry.setName(Plugin.EMPTY.equals(plugin.elementType()) ? alias : plugin.elementType());
                entry.setPrintable(plugin.printObject());
                entry.setDefer(plugin.deferChildren());
                entry.setCategory(plugin.category());
                entries.add(entry);
            }
            return entries;
        }
    }
}
