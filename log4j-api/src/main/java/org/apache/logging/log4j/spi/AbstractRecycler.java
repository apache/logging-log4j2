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
package org.apache.logging.log4j.spi;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class AbstractRecycler<V> implements Recycler<V> {

    private final Supplier<V> supplier;
    private final Consumer<V> cleaner;

    public AbstractRecycler(final Supplier<V> supplier, final Consumer<V> cleaner) {
        this.supplier = supplier;
        this.cleaner = cleaner;
    }

    protected final V createObject() {
        final V obj = supplier.get();
        if (obj instanceof RecyclerAware) {
            ((RecyclerAware<V>) obj).setRecycler(this);
        }
        return obj;
    }

    protected final void cleanObject(V obj) {
        if (cleaner != null) {
            cleaner.accept(obj);
        }
    }
}
