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
package org.apache.logging.log4j.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.di.resolver.PluginAttributeFactoryResolver;
import org.apache.logging.log4j.plugins.name.NameProvider;
import org.apache.logging.log4j.plugins.name.PluginAttributeNameProvider;
import org.apache.logging.log4j.util.Strings;

/**
 * Qualifier for a plugin attribute for configuration options of a plugin. A plugin attribute has a case-insensitive
 * name and a string value which may be converted to a different type when a {@link TypeConverter} plugin exists for
 * a target type. In a {@linkplain Plugin plugin class}, attributes can be injected via several means:
 *
 * <ul>
 *     <li>a static method annotated with {@link Factory} or some other {@link FactoryType} annotation can have
 *     parameters annotated with {@code @PluginAttribute}</li>
 *     <li>a constructor annotated with {@link Inject} can have parameters annotated with {@code @PluginAttribute}</li>
 *     <li>a field can be annotated with {@code PluginAttribute} — note that any default value specified for this
 *     field may be overwritten with the default value specified in this annotation when the configuration attribute
 *     has no specified value</li>
 *     <li>a method parameter can be annotated with {@code PluginAttribute}</li>
 *     <li>an instance returned from a static {@link Factory} method may have its fields and methods injected
 *     if the instance implements {@link Supplier}</li>
 * </ul>
 *
 * <p>Default values may be specified via one of the <code>default<var>Type</var></code> attributes depending on the
 * annotated type. Unlisted types that are supported by a corresponding
 * {@link TypeConverter} may use the {@link #defaultString()} attribute.</p>
 *
 * <p>Plugin attributes with sensitive data such as passwords should set {@link #sensitive()} to {@code true} to avoid
 * having their values output in debug logs.</p>
 *
 * @see PluginAttributeFactoryResolver
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE_USE})
@NameProvider(PluginAttributeNameProvider.class)
@QualifierType
public @interface PluginAttribute {

    /**
     * Specifies the default boolean value to use.
     * @return the default boolean value.
     */
    boolean defaultBoolean() default false;

    /**
     * Specifies the default byte value to use.
     * @return the default byte value;
     */
    byte defaultByte() default 0;

    /**
     * Specifies the default byte value to use.
     * @return the default char value.
     */
    char defaultChar() default 0;

    /**
     * Specifies the default {@link Class} value to use.
     * @return the default class value.
     */
    Class<?> defaultClass() default Object.class;

    /**
     * Specifies the default double floating point value to use.
     * @return the default double value;
     */
    double defaultDouble() default 0.0d;

    /**
     * Specifies the default floating point value to use.
     * @return the default float value.
     */
    float defaultFloat() default 0.0f;

    /**
     * Specifies the default integer value to use.
     * @return the default integer value.
     */
    int defaultInt() default 0;

    /**
     * Specifies the default long value to use.
     * @return the default long value;
     */
    long defaultLong() default 0L;

    /**
     * Specifies the default long value to use.
     * @return the default short value.
     */
    short defaultShort() default 0;

    /**
     * Specifies the default value this attribute should use if none is provided or if the provided value is invalid.
     * @return the default String value.
     */
    String defaultString() default Strings.EMPTY;

    /**
     * Specifies the name of the attribute (case-insensitive) this annotation corresponds to.
     * If blank, defaults to using reflection on the annotated element as such:
     *
     * <ul>
     *     <li>Field: uses the field name.</li>
     *     <li>Method: when named <code>set<var>XYZ</var></code> or <code>with<var>XYZ</var></code>, uses the rest
     *     (<var>XYZ</var>) of the method name. Otherwise, uses the name of the first parameter.</li>
     *     <li>Parameter: uses the parameter name.</li>
     * </ul>
     * @return the value;
     */
    String value() default Strings.EMPTY;

    /**
     * Indicates that this attribute is a sensitive one that shouldn't be logged directly. Such attributes will instead
     * be output as a hashed value.
     * @return true if the attribute should be considered sensitive.
     */
    boolean sensitive() default false;

}
