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
package org.apache.logging.log4j.kit.env;

import java.nio.charset.Charset;
import java.time.Duration;
import org.apache.logging.log4j.kit.env.internal.PropertiesUtilPropertyEnvironment;
import org.jspecify.annotations.Nullable;

/**
 * Represents the main access point to Log4j properties.
 * <p>
 *     It provides as typesafe way to access properties stored in multiple {@link PropertySource}s, type conversion
 *     methods and property aggregation methods (cf. {@link #getProperty(Class)}).
 * </p>
 */
public interface PropertyEnvironment {

    static PropertyEnvironment getGlobal() {
        return PropertiesUtilPropertyEnvironment.INSTANCE;
    }

    /**
     * Gets the named property as a boolean value. If the property matches the string {@code "true"} (case-insensitive),
     * then it is returned as the boolean value {@code true}. Any other non-{@code null} text in the property is
     * considered {@code false}.
     *
     * @param name the name of the property to look up
     * @return the boolean value of the property or {@code false} if undefined.
     */
    default boolean getBooleanProperty(final String name) {
        return getBooleanProperty(name, false);
    }

    /**
     * Gets the named property as a boolean value.
     *
     * @param name         the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the boolean value of the property or {@code defaultValue} if undefined.
     */
    Boolean getBooleanProperty(String name, Boolean defaultValue);

    /**
     * Gets the named property as a Charset value.
     *
     * @param name the name of the property to look up
     * @return the Charset value of the property or {@link Charset#defaultCharset()} if undefined.
     */
    @SuppressWarnings("null")
    default Charset getCharsetProperty(final String name) {
        return getCharsetProperty(name, Charset.defaultCharset());
    }

    /**
     * Gets the named property as a Charset value.
     *
     * @param name         the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the Charset value of the property or {@code defaultValue} if undefined.
     */
    Charset getCharsetProperty(String name, Charset defaultValue);

    /**
     * Gets the named property as a Class value.
     *
     * @param name         the name of the property to look up
     * @param upperBound the upper bound for the class
     * @return the Class value of the property or {@code null} if it can not be loaded.
     */
    <T> @Nullable Class<? extends T> getClassProperty(final String name, final Class<T> upperBound);

    /**
     * Gets the named property as a subclass of {@code upperBound}.
     *
     * @param name         the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @param upperBound the upper bound for the class
     * @return the Class value of the property or {@code defaultValue} if it can not be loaded.
     */
    <T> Class<? extends T> getClassProperty(String name, Class<? extends T> defaultValue, Class<T> upperBound);

    /**
     * Gets the named property as {@link Duration}.
     *
     * @param name The property name.
     * @return The value of the String as a Duration or {@link Duration#ZERO} if it was undefined or could not be parsed.
     */
    default Duration getDurationProperty(final String name) {
        return getDurationProperty(name, Duration.ZERO);
    }

    /**
     * Gets the named property as {@link Duration}.
     *
     * @param name The property name.
     * @param defaultValue The default value.
     * @return The value of the String as a Duration or {@code defaultValue} if it was undefined or could not be parsed.
     */
    Duration getDurationProperty(String name, Duration defaultValue);

    /**
     * Gets the named property as an integer.
     *
     * @param name         the name of the property to look up
     * @return the parsed integer value of the property or {@code 0} if it was undefined or could not be
     * parsed.
     */
    default int getIntegerProperty(final String name) {
        return getIntegerProperty(name, 0);
    }

    /**
     * Gets the named property as an integer.
     *
     * @param name         the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the parsed integer value of the property or {@code defaultValue} if it was undefined or could not be
     * parsed.
     */
    Integer getIntegerProperty(String name, Integer defaultValue);

    /**
     * Gets the named property as a long.
     *
     * @param name         the name of the property to look up
     * @return the parsed long value of the property or {@code 0} if it was undefined or could not be
     * parsed.
     */
    default long getLongProperty(final String name) {
        return getLongProperty(name, 0L);
    }

    /**
     * Gets the named property as a long.
     *
     * @param name         the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the parsed long value of the property or {@code defaultValue} if it was undefined or could not be parsed.
     */
    Long getLongProperty(String name, Long defaultValue);

    /**
     * Gets the named property as a String.
     *
     * @param name the name of the property to look up
     * @return the String value of the property or {@code null} if undefined.
     */
    @Nullable
    String getStringProperty(String name);

    /**
     * Gets the named property as a String.
     *
     * @param name         the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the String value of the property or {@code defaultValue} if undefined.
     */
    default String getStringProperty(final String name, final String defaultValue) {
        final String prop = getStringProperty(name);
        return (prop == null) ? defaultValue : prop;
    }

    /**
     * Binds properties to class {@code T}.
     * <p>
     *     The implementation should at least support binding Java records with a single public constructor and enums.
     * </p>
     * @param propertyClass a class annotated by {@link Log4jProperty}.
     * @return an instance of T with all JavaBean properties bound.
     */
    <T> T getProperty(final Class<T> propertyClass);
}
