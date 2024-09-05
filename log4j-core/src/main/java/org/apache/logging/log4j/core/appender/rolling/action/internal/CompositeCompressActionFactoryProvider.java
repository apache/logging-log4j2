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
package org.apache.logging.log4j.core.appender.rolling.action.internal;

import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactory;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactoryProvider;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.di.Key;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class CompositeCompressActionFactoryProvider implements CompressActionFactoryProvider {

    private final List<CompressActionFactoryProvider> delegates;

    public CompositeCompressActionFactoryProvider(final @Nullable Configuration configuration) {
        if (configuration != null) {
            delegates = configuration.getComponent(new @Namespace(CompressActionFactoryProvider.NAMESPACE) Key<>() {});
        } else {
            delegates = List.of(new JreCompressActionFactoryProvider());
        }
    }

    @Override
    public @Nullable CompressActionFactory createFactoryForAlgorithm(String extension) {
        return delegates.stream()
                .map(p -> p.createFactoryForAlgorithm(extension))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
