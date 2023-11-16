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
package org.apache.logging.log4j.core.config.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.logging.log4j.core.config.plugins.visitors.PluginAttributeVisitor;
import org.apache.logging.log4j.util.Strings;

/**
 * Identifies a Plugin Attribute and its default value. Note that only one of the defaultFoo attributes will be
 * used based on the type this annotation is attached to. Thus, for primitive types, the default<i>Type</i>
 * attribute will be used for some <i>Type</i>. However, for more complex types (including enums), the default
 * string value is used instead and should correspond to the string that would correctly convert to the appropriate
 * enum value using {@link Enum#valueOf(Class, String) Enum.valueOf}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@PluginVisitorStrategy(PluginAttributeVisitor.class)
public @interface PluginAttribute {

    /**
     * Specifies the default boolean value to use.
     */
    boolean defaultBoolean() default false;

    /**
     * Specifies the default byte value to use.
     */
    byte defaultByte() default 0;

    /**
     * Specifies the default byte value to use.
     */
    char defaultChar() default 0;

    /**
     * Specifies the default {@link Class} value to use.
     */
    Class<?> defaultClass() default Object.class;

    /**
     * Specifies the default double floating point value to use.
     */
    double defaultDouble() default 0.0d;

    /**
     * Specifies the default floating point value to use.
     */
    float defaultFloat() default 0.0f;

    /**
     * Specifies the default integer value to use.
     */
    int defaultInt() default 0;

    /**
     * Specifies the default long value to use.
     */
    long defaultLong() default 0L;

    /**
     * Specifies the default long value to use.
     */
    short defaultShort() default 0;

    /**
     * Specifies the default value this attribute should use if none is provided or if the provided value is invalid.
     */
    String defaultString() default Strings.EMPTY;

    // TODO: could we allow a blank value and infer the attribute name through reflection?
    /**
     * Specifies the name of the attribute (case-insensitive) this annotation corresponds to.
     */
    String value();

    /**
     * Indicates that this attribute is a sensitive one that shouldn't be logged directly. Such attributes will instead
     * be output as a hashed value.
     */
    boolean sensitive() default false;
}
