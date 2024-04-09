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
     * Gets the named property as a String.
     *
     * @param name the name of the property to look up
     * @return the String value of the property or {@code null} if undefined.
     */
    @Nullable
    String getProperty(String name);

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
