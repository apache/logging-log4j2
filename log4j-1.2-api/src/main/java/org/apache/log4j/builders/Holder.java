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
package org.apache.log4j.builders;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a place to hold values generated inside of a Lambda expression.
 *
 * @param <V> The type of object referred to by this reference.
 * @deprecated Use {@link AtomicReference}.
 */
@Deprecated
public class Holder<V> {
    private V value;

    public Holder() {}

    public Holder(final V defaultValue) {
        this.value = defaultValue;
    }

    public void set(final V value) {
        this.value = value;
    }

    public V get() {
        return value;
    }
}
