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

package org.apache.logging.log4j.plugins.defaults.bean;

import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.model.MetaConstructor;
import org.apache.logging.log4j.plugins.spi.model.MetaField;
import org.apache.logging.log4j.plugins.spi.model.MetaMethod;
import org.apache.logging.log4j.plugins.spi.scope.InitializationContext;

import java.util.Collection;

interface InjectionContext<T> {
    interface Factory {
        <T> InjectionContext<T> forConstructor(final MetaConstructor<T> constructor,
                                               final Collection<InjectionPoint<?>> injectionPoints);

        <D, T> InjectionContext<T> forField(final MetaField<D, T> field, final InjectionPoint<T> injectionPoint,
                                            final D declaringInstance);

        <D, T> InjectionContext<T> forMethod(final MetaMethod<D, T> method, final Collection<InjectionPoint<?>> injectionPoints,
                                             final D declaringInstance);

        <D, T> InjectionContext<T> forDisposerMethod(final MetaMethod<D, T> disposerMethod,
                                                     final Collection<InjectionPoint<?>> injectionPoints,
                                                     final D declaringInstance,
                                                     final Object producerInstance);
    }

    T invoke(final InitializationContext<?> context);
}
