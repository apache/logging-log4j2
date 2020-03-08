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

import org.apache.logging.log4j.plugins.spi.bean.InitializationContext;
import org.apache.logging.log4j.plugins.spi.bean.Scoped;

import java.util.Objects;

class LazyScopedInstance<T> implements ScopedInstance<T> {
    private final Scoped<T> scoped;
    private final InitializationContext<T> context;
    private volatile T instance;

    LazyScopedInstance(final Scoped<T> scoped, final InitializationContext<T> context) {
        this.scoped = Objects.requireNonNull(scoped);
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public Scoped<T> getScoped() {
        return scoped;
    }

    @Override
    public T getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = scoped.create(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void close() {
        scoped.destroy(instance, context);
        instance = null;
    }
}
