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

import org.apache.logging.log4j.plugins.di.spi.DependencyChain;
import org.apache.logging.log4j.plugins.di.spi.ResolvableKey;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * Exception thrown when an instance of a type is not injectable.
 */
public class NotInjectableException extends InjectException {
    public NotInjectableException(final Class<?> injectClass) {
        this(DependencyChain.empty(), injectClass);
    }

    public NotInjectableException(final Key<?> key) {
        this(DependencyChain.empty(), key);
    }

    public NotInjectableException(final Key<?> key, final DependencyChain dependencies) {
        this(dependencies, key);
    }

    public NotInjectableException(final ResolvableKey<?> resolvableKey) {
        this(resolvableKey.getDependencyChain(), resolvableKey.getKey());
    }

    private NotInjectableException(final DependencyChain chain, final Object target) {
        super(formatMessage(target, chain));
    }

    private static String formatMessage(final Object target, final DependencyChain dependencies) {
        final StringBuilder sb = new StringBuilder("No @Inject constructor or default constructor found for ");
        if (!dependencies.isEmpty()) {
            sb.append("chain ");
            for (final Key<?> dependency : dependencies) {
                dependency.formatTo(sb);
                sb.append(" -> ");
            }
        }
        StringBuilders.appendValue(sb, target);
        return sb.toString();
    }
}
