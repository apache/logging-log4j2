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
import org.apache.logging.log4j.plugins.util.Builder;
import org.apache.logging.log4j.status.StatusLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Optional;
import java.util.function.Function;

/**
 * Builder strategy for parsing and injecting a configuration node. Implementations should contain a default constructor
 * and must provide a {@link #build()} implementation. This provides type conversion based on the injection point via
 * {@link org.apache.logging.log4j.plugins.convert.TypeConverters}.
 *
 * @param <Ann> the Annotation type.
 * @param <Cfg> the Configuration type.
 */
public interface ConfigurationInjectionBuilder<Ann extends Annotation, Cfg> extends Builder<Object> {

    /**
     * Creates a ConfigurationInjectionBuilder instance for the given annotation class using metadata provided by the annotation's
     * {@link InjectionStrategy} annotation. This instance must be further populated with
     * data before being {@linkplain #build() built} to be useful.
     *
     * @param injectorType the Plugin annotation class to find a ConfigurationInjectionBuilder for.
     * @return a ConfigurationInjectionBuilder instance if one could be created or empty.
     */
    @SuppressWarnings("unchecked")
    static <Ann extends Annotation, Cfg> Optional<ConfigurationInjectionBuilder<Ann, Cfg>> findBuilderForInjectionStrategy(final Class<Ann> injectorType) {
        return Optional.ofNullable(injectorType.getAnnotation(InjectionStrategy.class))
                .flatMap(type -> {
                    try {
                        return Optional.of((ConfigurationInjectionBuilder<Ann, Cfg>) type.value().newInstance());
                    } catch (final Exception e) {
                        StatusLogger.getLogger().error("Error loading PluginBuilder [{}] for annotation [{}].", type.value(), injectorType, e);
                        return Optional.empty();
                    }
                });
    }

    /**
     * Sets the Annotation to be used for this. If the given Annotation is not compatible with this class's type, then
     * it is ignored.
     *
     * @param annotation the Annotation instance.
     * @return {@code this}.
     * @throws NullPointerException if the argument is {@code null}.
     */
    ConfigurationInjectionBuilder<Ann, Cfg> withAnnotation(Annotation annotation);

    /**
     * Sets the list of aliases to use for this injection. No aliases are required, however.
     *
     * @param aliases the list of aliases to use.
     * @return {@code this}.
     */
    ConfigurationInjectionBuilder<Ann, Cfg> withAliases(String... aliases);

    /**
     * Sets the class to convert the plugin value to for injection. This should correspond with a class obtained from
     * a factory method or builder class field. Not all ConfigurationInjectionBuilder implementations may need this value.
     *
     * @param conversionType the type to convert the plugin string to (if applicable).
     * @return {@code this}.
     * @throws NullPointerException if the argument is {@code null}.
     */
    ConfigurationInjectionBuilder<Ann, Cfg> withConversionType(Class<?> conversionType);

    /**
     * Sets the Member that this builder is being used for injection upon. For instance, this could be the Field
     * that is being used for injecting a value, or it could be the factory method being used to inject parameters
     * into.
     *
     * @param member the member this builder is parsing a value for.
     * @return {@code this}.
     */
    ConfigurationInjectionBuilder<Ann, Cfg> withMember(Member member);

    ConfigurationInjectionBuilder<Ann, Cfg> withStringSubstitutionStrategy(Function<String, String> stringSubstitutionStrategy);

    ConfigurationInjectionBuilder<Ann, Cfg> withDebugLog(StringBuilder debugLog);

    ConfigurationInjectionBuilder<Ann, Cfg> withConfiguration(Cfg configuration);

    ConfigurationInjectionBuilder<Ann, Cfg> withConfigurationNode(Node node);
}
