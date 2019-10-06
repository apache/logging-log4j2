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

package org.apache.logging.log4j.plugins.bind;

/**
 * Strategy to bind and validate an {@linkplain org.apache.logging.log4j.plugins.inject.ConfigurationInjector injected
 * configuration value} to a {@linkplain org.apache.logging.log4j.plugins.PluginFactory plugin factory}.
 */
public interface ConfigurationBinder {
    /**
     * Binds an unparsed string value to the given factory.
     *
     * @param factory injection factory to bind value to
     * @param value   string representation of configuration value
     * @throws ConfigurationBindingException if the given value is invalid
     */
    void bindString(final Object factory, final String value);

    /**
     * Binds an object to the given factory.
     *
     * @param factory injection factory to bind value to
     * @param value   configuration value to bind
     * @throws ConfigurationBindingException if the given value is invalid
     */
    void bindObject(final Object factory, final Object value);
}
