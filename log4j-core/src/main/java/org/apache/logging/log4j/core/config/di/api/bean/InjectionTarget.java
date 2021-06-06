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

public interface InjectionTarget<T> extends Producer<T> {
    // sets values of injected fields and calls initializer methods
    void inject(final T instance, final InitializationContext<T> context);

    // calls @PostConstruct from top to bottom
    void postConstruct(final T instance);

    // calls @PreDestroy from bottom to top
    void preDestroy(final T instance);

    @Override
    default void dispose(T instance) {
        // @Disposes only applies to @Produces methods and fields
    }
}
