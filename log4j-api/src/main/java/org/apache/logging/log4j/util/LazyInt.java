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

package org.apache.logging.log4j.util;

import java.util.function.IntSupplier;

public class LazyInt implements IntSupplier {
    private final IntSupplier supplier;
    private volatile boolean initialized;
    private volatile int value;

    public LazyInt(final IntSupplier supplier) {
        this.supplier = supplier;
    }

    @Override
    public int getAsInt() {
        boolean uninitialized = !initialized;
        int value = this.value;
        if (uninitialized) {
            synchronized (this) {
                uninitialized = !initialized;
                if (uninitialized) {
                    this.value = value = supplier.getAsInt();
                    initialized = true;
                }
            }
        }
        return value;
    }

    public synchronized void setAsInt(final int i) {
        initialized = false;
        value = i;
        initialized = true;
    }
}
