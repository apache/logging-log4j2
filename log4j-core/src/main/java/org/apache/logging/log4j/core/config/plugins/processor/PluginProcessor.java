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

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor7;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.util.Strings;

/**
 * Annotation processor for pre-scanning Log4j 2 plugins.
 */
@ServiceProvider(value = Processor.class, resolution = Resolution.OPTIONAL)
@SupportedAnnotationTypes("org.apache.logging.log4j.core.config.plugins.Plugin")
public class PluginProcessor extends AbstractProcessor {

    // TODO: this could be made more abstract to allow for compile-time and run-time plugin processing

    private static final Element[] EMPTY_ELEMENT_ARRAY = {};

    private static final String SUPPRESS_WARNING_PUBLIC_SETTER_STRING = "log4j.public.setter";

    /**
     * The location of the plugin cache data file. This file is written to by this processor, and read from by
     * {@link org.apache.logging.log4j.core.config.plugins.util.PluginManager}.
     */
    public static final String PLUGIN_CACHE_FILE =
            "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat";

    private final List<Element> processedElements = new ArrayList<>();
    private final PluginCache pluginCache = new PluginCache();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        final Messager messager = processingEnv.getMessager();
        // Process the elements for this round
        if (!annotations.isEmpty()) {
            final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Plugin.class);
            collectPlugins(elements);
            processedElements.addAll(elements);

            // process plugin builder Attributes
            final Set<? extends Element> pluginAttributeBuilderElements =
                    roundEnv.getElementsAnnotatedWith(PluginBuilderAttribute.class);
            processBuilderAttribute(pluginAttributeBuilderElements);
            processedElements.addAll(pluginAttributeBuilderElements);
        }
        // Write the cache file
        if (roundEnv.processingOver() && !processedElements.isEmpty()) {
            try {
                messager.printMessage(
                        Diagnostic.Kind.NOTE,
                        String.format(
                                "%s: writing plugin descriptor for %d Log4j Plugins to `%s`.",
                                PluginProcessor.class.getSimpleName(), processedElements.size(), PLUGIN_CACHE_FILE));
                writeCacheFile(processedElements.toArray(EMPTY_ELEMENT_ARRAY));
            } catch (final Exception e) {
                StringWriter sw = new StringWriter();
                sw.append(PluginProcessor.class.getSimpleName())
                        .append(": unable to write plugin descriptor to file ")
                        .append(PLUGIN_CACHE_FILE)
                        .append("\n");
                e.printStackTrace(new PrintWriter(sw));
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, sw.toString());
            }
        }
        // Do not claim the annotations to allow other annotation processors to run
        return false;
    }

    private void processBuilderAttribute(final Iterable<? extends Element> elements) {
        for (final Element element : elements) {
            if (element instanceof VariableElement) {
                processBuilderAttribute((VariableElement) element);
            }
        }
    }

    private void processBuilderAttribute(final VariableElement element) {
        final String fieldName = element.getSimpleName().toString(); // Getting the name of the field
        SuppressWarnings suppress = element.getAnnotation(SuppressWarnings.class);
        if (suppress != null && Arrays.asList(suppress.value()).contains(SUPPRESS_WARNING_PUBLIC_SETTER_STRING)) {
            // Suppress the warning due to annotation
            return;
        }
        final Element enclosingElement = element.getEnclosingElement();
        // `element is a field
        if (enclosingElement instanceof TypeElement) {
            final TypeElement typeElement = (TypeElement) enclosingElement;
            // Check the siblings of the field
            for (final Element enclosedElement : typeElement.getEnclosedElements()) {
                // `enclosedElement is a method or constructor
                if (enclosedElement instanceof ExecutableElement) {
                    final ExecutableElement methodElement = (ExecutableElement) enclosedElement;
                    final String methodName = methodElement.getSimpleName().toString();

                    if ((methodName.toLowerCase(Locale.ROOT).startsWith("set") // Typical pattern for setters
                                    || methodName
                                            .toLowerCase(Locale.ROOT)
                                            .startsWith("with")) // Typical pattern for setters
                            && methodElement.getParameters().size()
                                    == 1 // It is a weird pattern to not have public setter
                    ) {

                        Types typeUtils = processingEnv.getTypeUtils();

                        boolean followsNamePattern = methodName.equals(
                                        String.format("set%s", expectedFieldNameInASetter(fieldName)))
                                || methodName.equals(String.format("with%s", expectedFieldNameInASetter(fieldName)));

                        // Check if method is public
                        boolean isPublicMethod = methodElement.getModifiers().contains(Modifier.PUBLIC);

                        // Check if the return type of the method element is Assignable.
                        // Assuming it is a builder class the type of it should be assignable to its parent
                        boolean checkForAssignable = typeUtils.isAssignable(
                                methodElement.getReturnType(),
                                methodElement.getEnclosingElement().asType());

                        boolean foundPublicSetter = followsNamePattern && checkForAssignable && isPublicMethod;
                        if (foundPublicSetter) {
                            // Hurray we found a public setter for the field!
                            return;
                        }
                    }
                }
            }
            // If the setter was not found generate a compiler warning.
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            String.format(
                                    "The field `%s` does not have a public setter, Note that @SuppressWarnings(\"%s\"), can be used on the field to suppress the compilation error. ",
                                    fieldName, SUPPRESS_WARNING_PUBLIC_SETTER_STRING),
                            element);
        }
    }

    /**
     *  Helper method to get the expected Method name in a field.
     *  For example if the field name is 'isopen', then the expected setter would be 'setOpen' or 'withOpen'
     *  This method is supposed to return the capitalized 'Open', fieldName which is expected in the setter.
     * @param fieldName who's setter we are checking.
     * @return The expected fieldName that will come after withxxxx or setxxxx
     */
    private static String expectedFieldNameInASetter(String fieldName) {
        if (fieldName.startsWith("is")) fieldName = fieldName.substring(2);

        return fieldName.isEmpty() ? fieldName : Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    private void collectPlugins(final Iterable<? extends Element> elements) {
        final Elements elementUtils = processingEnv.getElementUtils();
        final ElementVisitor<PluginEntry, Plugin> pluginVisitor = new PluginElementVisitor(elementUtils);
        final ElementVisitor<Collection<PluginEntry>, Plugin> pluginAliasesVisitor =
                new PluginAliasesElementVisitor(elementUtils);
        for (final Element element : elements) {
            final Plugin plugin = element.getAnnotation(Plugin.class);
            if (plugin == null) {
                continue;
            }
            final PluginEntry entry = element.accept(pluginVisitor, plugin);
            final Map<String, PluginEntry> category = pluginCache.getCategory(entry.getCategory());
            category.put(entry.getKey(), entry);
            final Collection<PluginEntry> entries = element.accept(pluginAliasesVisitor, plugin);
            for (final PluginEntry pluginEntry : entries) {
                category.put(pluginEntry.getKey(), pluginEntry);
            }
        }
    }

    private void writeCacheFile(final Element... elements) throws IOException {
        final FileObject fileObject = processingEnv
                .getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, Strings.EMPTY, PLUGIN_CACHE_FILE, elements);
        try (final OutputStream out = fileObject.openOutputStream()) {
            pluginCache.writeCache(out);
        }
    }

    /**
     * ElementVisitor to scan the Plugin annotation.
     */
    private static final class PluginElementVisitor extends SimpleElementVisitor7<PluginEntry, Plugin> {

        private final Elements elements;

        private PluginElementVisitor(final Elements elements) {
            this.elements = elements;
        }

        @Override
        public PluginEntry visitType(final TypeElement e, final Plugin plugin) {
            Objects.requireNonNull(plugin, "Plugin annotation is null.");
            final PluginEntry entry = new PluginEntry();
            entry.setKey(toRootLowerCase(plugin.name()));
            entry.setClassName(elements.getBinaryName(e).toString());
            entry.setName(Plugin.EMPTY.equals(plugin.elementType()) ? plugin.name() : plugin.elementType());
            entry.setPrintable(plugin.printObject());
            entry.setDefer(plugin.deferChildren());
            entry.setCategory(plugin.category());
            return entry;
        }
    }

    /**
     * ElementVisitor to scan the PluginAliases annotation.
     */
    private static final class PluginAliasesElementVisitor
            extends SimpleElementVisitor7<Collection<PluginEntry>, Plugin> {

        private final Elements elements;

        private PluginAliasesElementVisitor(final Elements elements) {
            super(Collections.emptyList());
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
                entry.setKey(toRootLowerCase(alias));
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
