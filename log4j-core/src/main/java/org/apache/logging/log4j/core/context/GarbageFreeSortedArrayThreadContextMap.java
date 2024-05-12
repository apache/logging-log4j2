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
package org.apache.logging.log4j.core.context;

import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.StringMap;
import org.jspecify.annotations.Nullable;

/**
 * {@code SortedArrayStringMap}-based implementation of the {@code ThreadContextMap} interface that attempts not to
 * create temporary objects. Adding and removing key-value pairs will not create temporary objects.
 * <p>
 * This implementation does <em>not</em> make a copy of its contents on every operation, so this data structure cannot
 * be passed to log events. Instead, client code needs to copy the contents when interacting with another thread.
 * </p>
 * @since 2.7
 */
public class GarbageFreeSortedArrayThreadContextMap extends AbstractSortedArrayThreadContextMap {

    public GarbageFreeSortedArrayThreadContextMap() {
        this(PropertiesUtil.getProperties());
    }

    GarbageFreeSortedArrayThreadContextMap(final PropertiesUtil properties) {
        super(properties);
    }

    @Override
    protected @Nullable StringMap copyStringMap(@Nullable StringMap value) {
        return value != null ? createStringMap(value) : null;
    }

    @Override
    public void clear() {
        final StringMap map = localMap.get();
        if (map != null) {
            map.clear();
        }
    }
}
