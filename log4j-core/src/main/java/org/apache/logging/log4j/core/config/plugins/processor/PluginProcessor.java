/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor6;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.util.Strings;

/**
 * Annotation processor for pre-scanning Log4j 2 plugins.
 */
@SupportedAnnotationTypes("org.apache.logging.log4j.core.config.plugins.*")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PluginProcessor extends AbstractProcessor {

    // TODO: this could be made more abstract to allow for compile-time and run-time plugin processing

    /**
     * The location of the plugin cache data file. This file is written to by this processor, and read from by
     * {@link org.apache.logging.log4j.core.config.plugins.util.PluginManager}.
     */
    private static final String PLUGIN_DEFAULT_PACKAGE = "org.apache.logging.log4j.core.config.plugins";
    public static final String PLUGIN_CACHE_FILE = getResourceNameForPackage(PLUGIN_DEFAULT_PACKAGE);

    private final PluginCache pluginCache = new PluginCache();

    public static String getResourceNameForPackage(final String packge) {
        if (packge.isEmpty()) {
            throw new IllegalArgumentException(
                    "All annotated @Plugin classes must reside in a common non-default parent package");
        }
        return ("META-INF/" + packge.replace('.', '/') + "/Log4j2Plugins.dat").replace("//", "/");
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        try {
            final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Plugin.class);
            if (elements.isEmpty()) {
                return false;
            }
            collectPlugins(elements);
            writeCacheFile(elements.toArray(new Element[elements.size()]));
            return true;
        } catch (final IOException e) {
            error(e.getMessage());
            return false;
        }
    }

    private void error(final CharSequence message) {
        processingEnv.getMessager().printMessage(Kind.ERROR, message);
    }

    private void collectPlugins(final Iterable<? extends Element> elements) {
        final Elements elementUtils = processingEnv.getElementUtils();
        final ElementVisitor<PluginEntry, Plugin> pluginVisitor =
                new PluginElementVisitor(elementUtils);
        final ElementVisitor<Collection<PluginEntry>, Plugin> pluginAliasesVisitor =
                new PluginAliasesElementVisitor(elementUtils);
        for (final Element element : elements) {
            final Plugin plugin = element.getAnnotation(Plugin.class);
            final PluginEntry entry = element.accept(pluginVisitor, plugin);
            final ConcurrentMap<String, PluginEntry> category = pluginCache.getCategory(entry.getCategory());
            category.put(entry.getKey(), entry);
            final Collection<PluginEntry> entries = element.accept(pluginAliasesVisitor, plugin);
            for (final PluginEntry pluginEntry : entries) {
                category.put(pluginEntry.getKey(), pluginEntry);
            }
        }
    }

    private void writeCacheFile(final Element... elements) throws IOException {
        final String basePackage = findSharedPrefix(elements);
        final String cacheFile = getResourceNameForPackage(basePackage);
        final FileObject fo = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,
            Strings.EMPTY, cacheFile, elements);
        final OutputStream out = fo.openOutputStream();
        try {
            pluginCache.writeCache(out);
        } finally {
            out.close();
        }
    }

    private String findSharedPrefix(final Element... elements) {
        assert 0 != elements.length : "need at least one entry";
        CharSequence result = "";
        for(int i = 0 ; i < elements.length ; ++i) {
            CharSequence string = processingEnv.getElementUtils().getPackageOf(elements[i]).getQualifiedName();
            result = 0 == i ? string : findSharedPrefix(result, string);
        }
        return "org.apache.logging.log4j.core.".equals(result.toString())
                ? PLUGIN_DEFAULT_PACKAGE
                : result.toString();
    }

    private static CharSequence findSharedPrefix(final CharSequence a, final CharSequence b) {
        int minLength = Math.min(a.length(), b.length());
        for (int i = 0; i < minLength; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.subSequence(0, i);
            }
        }
        return a.subSequence(0, minLength);
    }

    /**
     * ElementVisitor to scan the Plugin annotation.
     */
    private static class PluginElementVisitor extends SimpleElementVisitor6<PluginEntry, Plugin> {

        private final Elements elements;

        private PluginElementVisitor(final Elements elements) {
            this.elements = elements;
        }

        @Override
        public PluginEntry visitType(final TypeElement e, final Plugin plugin) {
            if (plugin == null) {
                throw new NullPointerException("Plugin annotation is null.");
            }
            final PluginEntry entry = new PluginEntry();
            entry.setKey(plugin.name().toLowerCase());
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
    private static class PluginAliasesElementVisitor extends SimpleElementVisitor6<Collection<PluginEntry>, Plugin> {

        private final Elements elements;

        private PluginAliasesElementVisitor(final Elements elements) {
            super(Collections.<PluginEntry>emptyList());
            this.elements = elements;
        }

        @Override
        public Collection<PluginEntry> visitType(final TypeElement e, final Plugin plugin) {
            final PluginAliases aliases = e.getAnnotation(PluginAliases.class);
            if (aliases == null) {
                return DEFAULT_VALUE;
            }
            final Collection<PluginEntry> entries = new ArrayList<PluginEntry>(aliases.value().length);
            for (final String alias : aliases.value()) {
                final PluginEntry entry = new PluginEntry();
                entry.setKey(alias.toLowerCase());
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
