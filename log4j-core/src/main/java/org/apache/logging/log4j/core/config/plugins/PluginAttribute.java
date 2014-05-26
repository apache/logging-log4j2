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
package org.apache.logging.log4j.core.config.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a Plugin Attribute and its default value. Note that only one of the defaultFooValue attributes will be
 * used based on the type this annotation is attached to. Thus, for primitive types, the default<i>Type</i>Value
 * attribute will be used for some <i>Type</i>. However, for more complex types (including enums), the default
 * string value is used instead and should correspond to the string that would correctly convert to the appropriate
 * enum value using {@link Enum#valueOf(Class, String) Enum.valueOf}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface PluginAttribute {

    // TODO: could we allow a blank value and infer the attribute name through reflection?
    /**
     * Specifies the name of the attribute (case-insensitive) this annotation corresponds to.
     */
    String value();

    /**
     * Specifies the default value this attribute should use if none is provided or if the provided value is invalid.
     */
    String defaultStringValue() default "";

    /**
     * Specifies the default integer value to use.
     */
    int defaultIntValue() default 0;

    /**
     * Specifies the default long value to use.
     */
    long defaultLongValue() default 0L;

    /**
     * Specifies the default boolean value to use.
     */
    boolean defaultBooleanValue() default false;

    /**
     * Specifies the default floating point value to use.
     */
    float defaultFloatValue() default 0.0f;

    /**
     * Specifies the default double floating point value to use.
     */
    double defaultDoubleValue() default 0.0d;
}
