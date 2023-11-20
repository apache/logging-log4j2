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
package org.apache.logging.log4j.spi;

/**
 * Strategy for recycling objects. This is primarily useful for heavyweight objects and buffers.
 *
 * @param <V> the recyclable type
 * @since 3.0.0
 */
public interface Recycler<V> {

    /**
     * Acquires an instance of V. This may either be a fresh instance of V or a recycled instance of V.
     * Recycled instances will be modified by their cleanup function before being returned.
     *
     * @return an instance of V to be used
     */
    V acquire();

    /**
     * Releases an instance of V. This allows the instance to be recycled and later reacquired for new
     * purposes.
     *
     * @param value an instance of V no longer being used
     */
    void release(V value);
}
