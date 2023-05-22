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
package org.apache.logging.log4j.plugins.di;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.logging.log4j.plugins.ScopeType;
import org.apache.logging.log4j.plugins.di.spi.Scope;
import org.apache.logging.log4j.util.Cast;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomScopeTest {
    @Retention(RetentionPolicy.RUNTIME)
    @ScopeType
    @interface CustomSingleton {
    }

    @CustomSingleton
    static class CustomInstance {
    }

    static class CustomSingletonScope implements Scope {
        private final Map<Key<?>, Object> bindings = new ConcurrentHashMap<>();

        @Override
        public <T> Supplier<T> get(final Key<T> key, final Supplier<T> unscoped) {
            final Binding<T> binding = Binding.from(key).toSingleton(unscoped);
            return Cast.cast(bindings.computeIfAbsent(key, ignored -> binding));
        }
    }

    @Test
    void customScope() {
        final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();
        instanceFactory.registerScope(CustomSingleton.class, new CustomSingletonScope());
        final var factory = instanceFactory.getFactory(CustomInstance.class);
        assertThat(factory.get())
                .isSameAs(factory.get())
                .isSameAs(instanceFactory.getInstance(CustomInstance.class));
    }
}
