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

package org.apache.logging.log4j.plugins.util;

import java.util.function.Supplier;

public class LazyValue<T> implements Supplier<T> {

    public static <T> LazyValue<T> forSupplier(final Supplier<T> constructor) {
        return new LazyValue<>(constructor);
    }

    private final Supplier<T> constructor;
    private volatile T value;

    private LazyValue(final Supplier<T> constructor) {
        this.constructor = constructor;
    }

    @Override
    public T get() {
        T value = this.value;
        if (value == null) {
            synchronized (this) {
                value = this.value;
                if (value == null) {
                    this.value = value = constructor.get();
                }
            }
        }
        return value;
    }
}
