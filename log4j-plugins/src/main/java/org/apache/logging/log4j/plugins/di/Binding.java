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

import java.util.function.Supplier;

class Binding<T> {
    private final Key<T> key;
    private final Supplier<T> supplier;

    private Binding(final Key<T> key, final Supplier<T> supplier) {
        this.key = key;
        this.supplier = supplier;
    }

    public Key<T> getKey() {
        return key;
    }

    public Supplier<T> getSupplier() {
        return supplier;
    }

    public static <T> Binding<T> bind(final Key<T> key, final Supplier<T> supplier) {
        return new Binding<>(key, supplier);
    }
}
