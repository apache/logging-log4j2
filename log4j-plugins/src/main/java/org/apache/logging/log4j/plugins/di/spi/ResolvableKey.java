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
package org.apache.logging.log4j.plugins.di.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import org.apache.logging.log4j.lang.Nullable;
import org.apache.logging.log4j.plugins.di.Key;

public class ResolvableKey<T> {
    private final Key<T> key;
    private final Collection<String> aliases;
    private final DependencyChain dependencyChain;

    private ResolvableKey(final Key<T> key, final Collection<String> aliases, final DependencyChain dependencyChain) {
        this.key = key;
        this.aliases = aliases;
        this.dependencyChain = dependencyChain;
    }

    public Key<T> getKey() {
        return key;
    }

    public Type getType() {
        return key.getType();
    }

    public Class<T> getRawType() {
        return key.getRawType();
    }

    public String getName() {
        return key.getName();
    }

    public String getNamespace() {
        return key.getNamespace();
    }

    public @Nullable Class<? extends Annotation> getQualifierType() {
        return key.getQualifierType();
    }

    public Collection<String> getAliases() {
        return aliases;
    }

    public DependencyChain getDependencyChain() {
        return dependencyChain;
    }

    public static <T> ResolvableKey<T> of(final Key<T> key) {
        return new ResolvableKey<>(key, List.of(), DependencyChain.empty());
    }

    public static <T> ResolvableKey<T> of(final Key<T> key, final Collection<String> aliases) {
        return new ResolvableKey<>(key, aliases, DependencyChain.empty());
    }

    public static <T> ResolvableKey<T> of(final Key<T> key, final DependencyChain dependencyChain) {
        return new ResolvableKey<>(key, List.of(), dependencyChain);
    }

    public static <T> ResolvableKey<T> of(
            final Key<T> key, final Collection<String> aliases, final DependencyChain dependencyChain) {
        return new ResolvableKey<>(key, aliases, dependencyChain);
    }
}
