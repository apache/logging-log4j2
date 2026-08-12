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
package org.apache.logging.log4j.config.spi;

import java.util.Map;

/**
 * Service provider interface for a property environment shared across configuration components.
 * <p>
 * Provides a vocabulary for reading configuration and context properties without referencing
 * core lookup or substitutor implementations.
 * </p>
 *
 * @since 3.0.0
 */
public interface PropertyEnvironmentSPI {

    /**
     * Returns the value of a property, or {@code null} if not defined.
     *
     * @param key the property key
     * @return the property value, or {@code null}
     */
    String getProperty(String key);

    /**
     * Returns all properties available in this environment.
     *
     * @return a map of property names to values
     */
    Map<String, String> getProperties();

    /**
     * Returns whether a property is defined in this environment.
     *
     * @param key the property key
     * @return {@code true} if the property is defined
     */
    boolean containsProperty(String key);
}
