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

import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.model.MetaConstructor;
import org.apache.logging.log4j.plugins.spi.model.MetaField;
import org.apache.logging.log4j.plugins.spi.model.MetaMethod;
import org.apache.logging.log4j.plugins.spi.scope.InitializationContext;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public interface InjectionFactory {
    <T> Function<InitializationContext<T>, T> forConstructor(final MetaConstructor<T> constructor,
                                                             final Collection<InjectionPoint<?>> injectionPoints);

    <D, T> Function<InitializationContext<T>, T> forProducerMethod(final MetaMethod<D, T> producerMethod,
                                                                   final D declaringInstance,
                                                                   final Collection<InjectionPoint<?>> injectionPoints);

    <T> Consumer<InitializationContext<T>> forDisposerMethod(final MetaMethod<T, ?> disposerMethod,
                                                             final T declaringInstance,
                                                             final Collection<InjectionPoint<?>> injectionPoints,
                                                             final Object producedInstance);

    <D, T> Consumer<InitializationContext<D>> forField(final MetaField<D, T> field, final D declaringInstance,
                                                       final InjectionPoint<T> injectionPoint);

    <D, T> Consumer<InitializationContext<D>> forMethod(final MetaMethod<D, T> method, final D declaringInstance,
                                                        final Collection<InjectionPoint<?>> injectionPoints);
}
