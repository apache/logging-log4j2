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

package org.apache.logging.log4j.plugins.spi.bean;

import org.apache.logging.log4j.plugins.api.ScopeType;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Manages {@link Scoped} instances within a particular {@linkplain ScopeType scope}.
 */
public interface ScopeContext extends AutoCloseable {

    /**
     * Returns the {@linkplain ScopeType scope type} of this context.
     */
    Class<? extends Annotation> getScopeType();

    /**
     * Gets or {@linkplain Scoped#create(InitializationContext) creates} a scoped instance of a specific type.
     *
     * @param scoped the managed type
     * @param context the context to create a new instance in
     * @param <T>     the instance type being managed
     * @return the new or existing instance
     */
    <T> T getOrCreate(final Scoped<T> scoped, final InitializationContext<T> context);

    /**
     * Returns an existing scoped instance if it exists.
     *
     * @param scoped the managed type
     * @param <T>     the instance type being managed
     * @return the existing instance or empty
     */
    <T> Optional<T> getIfExists(final Scoped<T> scoped);

    /**
     * Destroys the existing scoped instance of a specified type if it exists or otherwise does nothing.
     *
     * @param scoped the managed type
     */
    void destroy(final Scoped<?> scoped);

    @Override
    void close();
}
