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

package org.apache.logging.log4j.core.config.plugins.visitors;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginNode;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.core.config.plugins.SensitivePluginAttribute;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Registry for associating Plugin annotations with PluginVisitor implementations.
 */
public final class PluginVisitors {

    private static final Logger LOGGER = StatusLogger.getLogger();

    // map of annotation classes to their corresponding PluginVisitor classes
    // generics are fun!
    private static final Map<Class<? extends Annotation>, Class<? extends PluginVisitor<? extends Annotation>>> REGISTRY;

    static {
        // register the default PluginVisitor classes
        REGISTRY = new ConcurrentHashMap<Class<? extends Annotation>, Class<? extends PluginVisitor<? extends Annotation>>>();
        registerVisitor(PluginAttribute.class, PluginAttributeVisitor.class);
        registerVisitor(SensitivePluginAttribute.class, SensitivePluginAttributeVisitor.class);
        registerVisitor(PluginConfiguration.class, PluginConfigurationVisitor.class);
        registerVisitor(PluginNode.class, PluginNodeVisitor.class);
        registerVisitor(PluginValue.class, PluginValueVisitor.class);
        registerVisitor(PluginElement.class, PluginElementVisitor.class);
    }

    private PluginVisitors() {
    }

    /**
     * Registers a PluginVisitor class associated to a specific annotation.
     *
     * @param annotation the Plugin annotation to associate with.
     * @param helper     the PluginVisitor class to use for the annotation.
     * @param <A>        the Plugin annotation type.
     */
    public static <A extends Annotation> void registerVisitor(final Class<A> annotation,
                                                              final Class<? extends PluginVisitor<A>> helper) {
        REGISTRY.put(annotation, helper);
    }

    /**
     * Creates a PluginVisitor instance for the given annotation class. This instance must be further populated with
     * data to be useful. Such data is passed through both the setters and the visit method.
     *
     * @param annotation the Plugin annotation class to find a PluginVisitor for.
     * @param <A>        the Plugin annotation type.
     * @return a PluginVisitor instance if one could be created, or {@code null} otherwise.
     */
    @SuppressWarnings("unchecked") // we're keeping track of types, thanks
    public static <A extends Annotation> PluginVisitor<A> findVisitor(final Class<A> annotation) {
        try {
            final Class<PluginVisitor<A>> clazz = (Class<PluginVisitor<A>>) REGISTRY.get(annotation);
            if (clazz == null) {
                return null;
            }
            return clazz.newInstance();
        } catch (final Exception e) {
            LOGGER.debug("No PluginVisitor found for annotation: {}.", annotation);
            return null;
        }
    }
}
