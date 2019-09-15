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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.convert.TypeConverters;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Base class for InjectionStrategyBuilder implementations. Provides fields and setters for the builder leaving only
 * {@link ConfigurationInjectionBuilder#build()} to be implemented.
 *
 * @param <Ann> the Plugin annotation type.
 */
public abstract class AbstractConfigurationInjectionBuilder<Ann extends Annotation, Cfg> implements ConfigurationInjectionBuilder<Ann, Cfg> {

    /** Status logger. */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * 
     */
    protected final Class<Ann> clazz;
    /**
     * 
     */
    protected Ann annotation;
    /**
     * 
     */
    protected String[] aliases;
    /**
     * 
     */
    protected Class<?> conversionType;
    /**
     * 
     */
    protected Member member;

    protected Cfg configuration;

    protected Node node;

    protected Function<String, String> stringSubstitutionStrategy;

    protected StringBuilder debugLog;

    /**
     * This constructor must be overridden by implementation classes as a no-arg constructor.
     *
     * @param clazz the annotation class this PluginVisitor is for.
     */
    protected AbstractConfigurationInjectionBuilder(final Class<Ann> clazz) {
        this.clazz = clazz;
    }

    @Override
    public ConfigurationInjectionBuilder<Ann, Cfg> withAnnotation(final Annotation anAnnotation) {
        final Annotation a = Objects.requireNonNull(anAnnotation, "No annotation was provided");
        if (this.clazz.isInstance(a)) {
            this.annotation = clazz.cast(a);
        }
        return this;
    }

    @Override
    public ConfigurationInjectionBuilder<Ann, Cfg> withAliases(final String... someAliases) {
        this.aliases = someAliases;
        return this;
    }

    @Override
    public ConfigurationInjectionBuilder<Ann, Cfg> withConversionType(final Class<?> aConversionType) {
        this.conversionType = Objects.requireNonNull(aConversionType, "No conversion type class was provided");
        return this;
    }

    @Override
    public ConfigurationInjectionBuilder<Ann, Cfg> withMember(final Member aMember) {
        this.member = aMember;
        return this;
    }

    @Override
    public ConfigurationInjectionBuilder<Ann, Cfg> withConfiguration(final Cfg aConfiguration) {
        this.configuration = aConfiguration;
        return this;
    }

    @Override
    public ConfigurationInjectionBuilder<Ann, Cfg> withStringSubstitutionStrategy(final Function<String, String> aStringSubstitutionStrategy) {
        this.stringSubstitutionStrategy = aStringSubstitutionStrategy;
        return this;
    }

    @Override
    public ConfigurationInjectionBuilder<Ann, Cfg> withDebugLog(final StringBuilder aDebugLog) {
        this.debugLog = aDebugLog;
        return this;
    }

    @Override
    public ConfigurationInjectionBuilder<Ann, Cfg> withConfigurationNode(final Node aNode) {
        this.node = aNode;
        return this;
    }

    /**
     * Removes an Entry from a given Map using a key name and aliases for that key. Keys are case-insensitive.
     *
     * @param attributes the Map to remove an Entry from.
     * @param name       the key name to look up.
     * @param aliases    optional aliases of the key name to look up.
     * @return the value corresponding to the given key or {@code null} if nonexistent.
     */
    protected static String removeAttributeValue(final Map<String, String> attributes,
                                                 final String name,
                                                 final String... aliases) {
        for (final Map.Entry<String, String> entry : attributes.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (key.equalsIgnoreCase(name)) {
                attributes.remove(key);
                return value;
            }
            if (aliases != null) {
                for (final String alias : aliases) {
                    if (key.equalsIgnoreCase(alias)) {
                        attributes.remove(key);
                        return value;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Converts the given value into the configured type falling back to the provided default value.
     *
     * @param value        the value to convert.
     * @param defaultValue the fallback value to use in case of no value or an error.
     * @return the converted value whether that be based on the given value or the default value.
     */
    protected Object convert(final String value, final Object defaultValue) {
        if (defaultValue instanceof String) {
            return TypeConverters.convert(value, this.conversionType, Strings.trimToNull((String) defaultValue));
        }
        return TypeConverters.convert(value, this.conversionType, defaultValue);
    }
}
