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

package org.apache.logging.log4j.core.config.di.api.bean;

import org.apache.logging.log4j.plugins.di.Inject;
import org.apache.logging.log4j.plugins.di.PostConstruct;
import org.apache.logging.log4j.plugins.di.PreDestroy;

/**
 * Provides lifecycle and dependency injection operations for instances of a specified type.
 *
 * @param <T> type of instance managed by this injection target
 */
public interface InjectionTarget<T> extends Producer<T> {

    /**
     * Performs dependency injection on the provided instance in the given initialization context. This sets the values
     * of injected fields and calls initializer methods (i.e., fields and methods annotated with {@link Inject}).
     *
     * @param instance instance upon which to perform dependency injection
     * @param context  initialization context to perform injection in
     */
    void inject(final T instance, final InitializationContext<T> context);

    /**
     * Invokes the {@link PostConstruct} methods of the provided instance.
     * Post construct methods are invoked starting with the highest level method and continue down the type hierarchy.
     *
     * @param instance instance upon which to invoke post construct methods
     */
    void postConstruct(final T instance);

    /**
     * Invokes the {@link PreDestroy} methods of the provided instance.
     * Pre destroy methods are invoked starting with the lowest level method and continue up the type hierarchy.
     *
     * @param instance instance upone which to invoke pre destroy methods
     */
    void preDestroy(final T instance);

    /**
     * Does nothing. This method only applies to producers.
     */
    @Override
    default void dispose(T instance) {
        // @Disposes only applies to @Produces methods and fields
    }
}
