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
import org.apache.logging.log4j.spi.ContextData;
import org.apache.logging.log4j.spi.MutableContextData;

/**
 * Responsible for initializing the ContextData of LogEvents. Context data is data that is set by the application to be
 * included in all subsequent log events.
 * <p>
 * The source of the context data is implementation-specific. The default source for context data is the ThreadContext.
 * </p><p>
 * In some asynchronous models, work may be delegated to several threads, while conceptually this work shares the same
 * context. In such models, storing context data in {@code ThreadLocal} variables is not convenient or desirable.
 * By specifying a custom {@code ContextDataInjectorFactory}, users can initialize log events with context data from
 * any arbitrary context.
 * </p>
 *
 * @see ContextData
 * @see ContextDataInjectorFactory
 * @see org.apache.logging.log4j.core.ContextData
 * @see org.apache.logging.log4j.ThreadContext
 * @see ThreadContextDataInjector
 * @since 2.7
 */
public interface ContextDataInjector {
    /**
     * Returns a {@code MutableContextData} object initialized with the specified properties and the appropriate
     * context data. The returned value may be the specified parameter or a different object.
     *
     * @param properties Properties from the log4j configuration to be added to the resulting ContextData
     * @param reusable a {@code MutableContextData} instance that may be reused to avoid creating temporary objects
     * @return a {@code MutableContextData} instance initialized with the specified properties and the appropriate
     *          context data. The returned value may be the specified parameter or a different object.
     */
    MutableContextData injectContextData(final List<Property> properties, final MutableContextData reusable);
}
