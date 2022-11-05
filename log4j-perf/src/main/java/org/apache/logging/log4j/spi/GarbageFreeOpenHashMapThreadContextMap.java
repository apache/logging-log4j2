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

import org.apache.logging.log4j.perf.nogc.OpenHashStringMap;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * {@code OpenHashStringMap}-based implementation of the {@code ThreadContextMap} interface that attempts not to
 * create temporary objects. Adding and removing key-value pairs will not create temporary objects.
 * <p>
 * Since the underlying data structure is modified directly it is not suitable for passing by reference to other
 * threads. Instead, client code needs to copy the contents when interacting with another thread.
 * </p>
 *
 * @since 2.7
 */
public class GarbageFreeOpenHashMapThreadContextMap extends GarbageFreeSortedArrayThreadContextMap {

    /** Constant used in benchmark code */
    public static final Class<? extends ThreadContextMap> SUPER = GarbageFreeSortedArrayThreadContextMap.class;

    @Override
    protected StringMap createStringMap() {
        return new OpenHashStringMap<>(PropertiesUtil.getProperties().getIntegerProperty(
                PROPERTY_NAME_INITIAL_CAPACITY, DEFAULT_INITIAL_CAPACITY));
    }

    @Override
    protected StringMap createStringMap(final ReadOnlyStringMap original) {
        return new OpenHashStringMap<>(original);
    }
}
