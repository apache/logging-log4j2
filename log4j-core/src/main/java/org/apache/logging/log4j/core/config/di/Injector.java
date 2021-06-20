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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

public interface Injector {
    <T> T construct(final Constructor<T> constructor, final Collection<InjectionPoint> points,
                    final InitializationContext<T> context);

    <D, T> T produce(final D producerInstance, final Method producerMethod,
                     final Collection<InjectionPoint> points, final InitializationContext<D> context);

    <T> void dispose(final T disposerInstance, final Method disposerMethod,
                     final Collection<InjectionPoint> points, final Object instance,
                     final InitializationContext<T> context);

    <T> void invoke(final T instance, final Method method, final Collection<InjectionPoint> points,
                    final InitializationContext<T> context);

    <D, T> void set(final D instance, final Field field, final InjectionPoint point,
                    final InitializationContext<D> context);
}
