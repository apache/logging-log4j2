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
 * Exception thrown when a {@linkplain org.apache.logging.log4j.plugins.QualifierType qualified} injection point
 * is attempted to be {@linkplain ResolvableKey resolved} and no binding has been registered previously.
 */
public class NoQualifiedBindingException extends InjectException {
    /**
     * Constructs an exception for the provided resolvable key.
     */
    public NoQualifiedBindingException(final ResolvableKey<?> key) {
        super(formatMessage(key));
    }

    private static String formatMessage(final ResolvableKey<?> key) {
        final StringBuilder sb = new StringBuilder("No qualified binding registered for ");
        final DependencyChain dependencyChain = key.dependencyChain();
        if (!dependencyChain.isEmpty()) {
            sb.append("chain ");
            for (final Key<?> dependency : dependencyChain) {
                dependency.formatTo(sb);
                sb.append(" -> ");
            }
        }
        StringBuilders.appendValue(sb, key.key());
        return sb.toString();
    }
}
