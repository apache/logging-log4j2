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

package org.apache.logging.log4j.plugins.inject;

import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.bind.ConfigurationBinder;
import org.apache.logging.log4j.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;

/**
 * Strategy builder for injecting configuration data into an {@link AnnotatedElement}. Configuration injection consists
 * of {@linkplain ConfigurationBinder binding} a {@link Node} and configuration to an annotated element of a
 * {@linkplain org.apache.logging.log4j.plugins.PluginFactory plugin factory}.
 *
 * @param <Ann> plugin annotation this injector uses
 * @param <Cfg> configuration class
 */
public interface ConfigurationInjector<Ann extends Annotation, Cfg> {

    static <Cfg> Optional<ConfigurationInjector<Annotation, Cfg>> forAnnotatedElement(final AnnotatedElement element) {
        for (final Annotation annotation : element.getAnnotations()) {
            final InjectorStrategy strategy = annotation.annotationType().getAnnotation(InjectorStrategy.class);
            if (strategy != null) {
                @SuppressWarnings("unchecked") final ConfigurationInjector<Annotation, Cfg> injector =
                        (ConfigurationInjector<Annotation, Cfg>) ReflectionUtil.instantiate(strategy.value());
                return Optional.of(injector.withAnnotatedElement(element).withAnnotation(annotation));
            }
        }
        return Optional.empty();
    }

    ConfigurationInjector<Ann, Cfg> withAnnotation(final Ann annotation);

    ConfigurationInjector<Ann, Cfg> withAnnotatedElement(final AnnotatedElement element);

    ConfigurationInjector<Ann, Cfg> withConversionType(final Type type);

    ConfigurationInjector<Ann, Cfg> withName(final String name);

    ConfigurationInjector<Ann, Cfg> withAliases(final String... aliases);

    ConfigurationInjector<Ann, Cfg> withConfigurationBinder(final ConfigurationBinder binder);

    ConfigurationInjector<Ann, Cfg> withDebugLog(final StringBuilder debugLog);

    ConfigurationInjector<Ann, Cfg> withStringSubstitutionStrategy(final Function<String, String> strategy);

    ConfigurationInjector<Ann, Cfg> withConfiguration(final Cfg configuration);

    ConfigurationInjector<Ann, Cfg> withNode(final Node node);

    void inject(final Object factory);
}
