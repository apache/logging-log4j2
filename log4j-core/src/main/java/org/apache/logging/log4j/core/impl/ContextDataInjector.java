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
package org.apache.logging.log4j.core.impl;

import java.util.List;

import org.apache.logging.log4j.core.config.Property;

/**
 * Responsible for initializing the ContextData of LogEvents. Context data is data that is set by the application to be
 * included in all subsequent log events.
 *
 * @see org.apache.logging.log4j.core.ContextData
 * @see org.apache.logging.log4j.ThreadContext
 * @since 2.7
 */
public interface ContextDataInjector {
    /**
     * Updates the specified ContextData with context key-value pairs.
     *
     * @param properties Properties from the configuration to be added to the ContextData
     * @param contextData the ContextData to initialize
     */
    void injectContextData(final List<Property> properties, final MutableContextData contextData);
}
