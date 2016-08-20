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
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * {@code SortedArrayContextData}-based implementation of the {@code ThreadContextMap} interface that creates a copy of
 * the data structure on every modification. Any particular instance of the data structure is a snapshot of the
 * ThreadContext at some point in time and can safely be passed off to other threads
 *
 * @since 2.7
 */
public class CopyOnWriteSortedArrayThreadContextMap extends AbstractCopyOnWriteMutableThreadContext {
    @Override
    protected MutableContextData createMutableContextData() {
        return new ArrayContextData(PropertiesUtil.getProperties().getIntegerProperty(
                PROPERTY_NAME_INITIAL_CAPACITY, DEFAULT_INITIAL_CAPACITY));
    }

    @Override
    protected MutableContextData createMutableContextData(final ContextData original) {
        return new ArrayContextData(original);
    }
}
