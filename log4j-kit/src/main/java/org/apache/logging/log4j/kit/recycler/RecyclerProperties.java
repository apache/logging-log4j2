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
package org.apache.logging.log4j.kit.recycler;

import org.apache.logging.log4j.kit.env.Log4jProperty;
import org.apache.logging.log4j.status.StatusLogger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A set of common configuration options for recyclers
 *
 * @param factory  The name of the recycler factory to use (cf. {@link RecyclerFactoryProvider#getName()}),
 * @param capacity The capacity of the recycler.
 */
@NullMarked
@Log4jProperty(name = "recycler")
public record RecyclerProperties(@Nullable String factory, @Nullable Integer capacity) {
    /**
     * The default recycler capacity: {@code max(2C+1, 8)}, {@code C} denoting the number of available processors
     */
    private static final int DEFAULT_CAPACITY =
            Math.max(2 * Runtime.getRuntime().availableProcessors() + 1, 8);

    public RecyclerProperties {
        capacity = validateCapacity(capacity);
    }

    private static Integer validateCapacity(final @Nullable Integer capacity) {
        if (capacity != null) {
            if (capacity >= 1) {
                return capacity;
            }
            StatusLogger.getLogger()
                    .warn("Invalid recycler capacity {}, using default capacity {}.", capacity, DEFAULT_CAPACITY);
        }
        return DEFAULT_CAPACITY;
    }
}
