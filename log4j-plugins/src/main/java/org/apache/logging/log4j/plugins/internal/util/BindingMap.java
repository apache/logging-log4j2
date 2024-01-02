/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.plugins.internal.util;

import java.util.function.Supplier;
import org.apache.logging.log4j.lang.Nullable;
import org.apache.logging.log4j.plugins.di.Key;

public sealed interface BindingMap permits DefaultBindingMap {
    <T> @Nullable Supplier<T> get(final Key<T> key, final Iterable<String> aliases);

    <T> void put(final Key<? super T> key, final Supplier<T> factory);

    <T> void putIfAbsent(final Key<? super T> key, final Supplier<T> factory);

    void remove(final Key<?> key);

    boolean containsKey(final Key<?> key);

    boolean containsLocalKey(final Key<?> key);

    BindingMap newChildMap();

    static BindingMap newRootMap() {
        return new DefaultBindingMap(HierarchicalMap.newRootMap());
    }
}
