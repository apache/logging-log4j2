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

package org.apache.logging.log4j.plugins.spi.scope;

import org.apache.logging.log4j.plugins.api.PrototypeScoped;
import org.apache.logging.log4j.plugins.api.SingletonScoped;

import java.lang.annotation.Annotation;

/**
 * Manages the creation and destruction of instances of a specific type within a particular scope context. Scopes
 * provide managed life-cycles for objects such as {@linkplain SingletonScoped singleton}
 * and {@linkplain PrototypeScoped prototype}.
 *
 * @param <T> type of managed instance
 * @see org.apache.logging.log4j.plugins.api.ScopeType
 */
public interface Scoped<T> {

    /**
     * Creates a new instance of this managed type. The given {@link InitializationContext} should be used by implementations
     * to track dependent objects.
     *
     * @param context the context in which the instance is being managed
     * @return a managed, initialized instance
     */
    T create(final InitializationContext<T> context);

    /**
     * Destroys a managed instance in the given context. Implementations should call {@link InitializationContext#close()} to
     * allow dependent objects to be destroyed.
     *
     * @param instance the managed instance to destroy
     * @param context  the context in which the instance is being managed
     */
    void destroy(final T instance, final InitializationContext<T> context);

    Class<? extends Annotation> getScopeType();

    default boolean isPrototypeScoped() {
        return getScopeType() == PrototypeScoped.class;
    }
}
