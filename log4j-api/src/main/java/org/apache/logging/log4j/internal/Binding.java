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

package org.apache.logging.log4j.internal;

import java.util.function.Supplier;

/**
 * Combination of a class key and a supplier function to get a bound value for the key.
 * @param <T> type of key
 */
public class Binding<T> {
    private final Class<T> key;
    private final Supplier<? extends T> supplier;

    private Binding(final Class<T> key, final Supplier<? extends T> supplier) {
        this.key = key;
        this.supplier = supplier;
    }

    public Class<T> getKey() {
        return key;
    }

    public Supplier<? extends T> getSupplier() {
        return supplier;
    }

    public static <T> Binding<T> bind(final Class<T> key, final Supplier<? extends T> supplier) {
        return new Binding<>(key, supplier);
    }
}
