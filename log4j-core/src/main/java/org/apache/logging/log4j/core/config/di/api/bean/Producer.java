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

import org.apache.logging.log4j.core.config.di.api.model.InjectionPoint;

import java.util.Collection;

public interface Producer<T> {
    // for a class: calls @Inject constructor or default constructor
    // for a producer method, this is invoked on that instance
    // for a producer field, the value is gotten from that instance
    T produce(InitializationContext<T> context);

    // for a class, no-op (preDestroy is relevant there instead)
    // for a producer method or producer field, calls the disposer method
    void dispose(T instance);

    // for a class: injected fields, @Inject constructor parameters, and initializer method params
    // for a producer method: method params
    Collection<InjectionPoint> getInjectionPoints();
}
