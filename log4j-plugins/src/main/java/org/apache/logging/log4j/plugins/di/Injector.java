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

package org.apache.logging.log4j.plugins.di;

import org.apache.logging.log4j.plugins.Node;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

public interface Injector {
    <T> Supplier<T> getFactory(final Class<T> clazz);

    <T> Supplier<T> getFactory(final Key<T> key);

    <T> T getInstance(final Class<T> clazz);

    <T> T getInstance(final Key<T> key);

    <T> T getInstance(final Node node);

    void injectNode(final Node node);

    boolean hasNoBindings(final Key<?> key);

    void removeBinding(final Key<?> key);

    void installModule(final Object module);

    void setCallerContext(final ReflectionCallerContext callerContext);

    void bindScope(final Class<? extends Annotation> scopeType, final Scope scope);

    void init();

    <T> Injector bindFactory(final Key<? super T> key, final Supplier<T> factory);

    <T> Injector bindIfMissing(final Key<? super T> key, final Supplier<T> factory);

    <T> Injector bindInstance(final Key<? super T> key, final T instance);
}
