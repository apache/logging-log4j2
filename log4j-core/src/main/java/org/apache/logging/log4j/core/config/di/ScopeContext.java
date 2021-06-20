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

package org.apache.logging.log4j.core.config.di;

import org.apache.logging.log4j.plugins.di.ScopeType;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Manages {@link Bean} instances within a particular {@linkplain ScopeType scope}.
 */
public interface ScopeContext extends AutoCloseable {

    /**
     * Returns the {@linkplain ScopeType scope type} of this context.
     */
    Class<? extends Annotation> getScopeType();

    /**
     * Gets or {@linkplain Bean#create(InitializationContext) creates} a bean instance of a specific type.
     *
     * @param bean    the bean to get or create an instance of
     * @param context the context to create a new instance in
     * @param <T>     the instance type being managed
     * @return the new or existing instance
     */
    <T> T getOrCreate(final Bean<T> bean, final InitializationContext<T> context);

    /**
     * Returns an existing bean instance if it exists.
     *
     * @param bean the bean to look up an existing instance of
     * @param <T>  the instance type being managed
     * @return the existing instance or empty
     */
    <T> Optional<T> getIfExists(final Bean<T> bean);

    /**
     * Destroys the existing bean instance of a specified type if it exists or otherwise does nothing.
     *
     * @param bean the managed type
     */
    void destroy(final Bean<?> bean);

    @Override
    void close();
}
