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

/**
 * Interface for objects that know how to provide a {@code MutableContextData} object.
 * <p>
 * This interface offers no guarantee that the returned context data is actually mutable; it may have been
 * {@linkplain MutableContextData#freeze() frozen}, making the data structure read-only.
 * </p>
 *
 * @since 2.7
 */
public interface MutableContextDataSupplier {

    /**
     * Returns the {@code MutableContextData}. Note that the returned context data may not be mutable; it may have been
     * {@linkplain MutableContextData#freeze() frozen}, making the data structure read-only.
     *
     * @return the {@code MutableContextData}
     */
    MutableContextData getMutableContextData();
}
