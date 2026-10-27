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
package org.apache.logging.log4j.core.appender.nosql;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Facilitates implementations of {@link NoSqlConnection}.
 *
 * @param <W>
 *            See {@link NoSqlConnection}.
 * @param <T>
 *            See {@link NoSqlConnection}.
 */
public abstract class AbstractNoSqlConnection<W, T extends NoSqlObject<W>> implements NoSqlConnection<W, T> {

    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Loads a reserved-field regular expression from a resource relative to the provider class.
     *
     * @param owner the provider class.
     * @param resourceName the resource name.
     * @return the compiled pattern for full-string matching.
     * @throws IllegalStateException if the resource or pattern is missing or invalid.
     * @since 2.27.0
     */
    protected static Pattern loadReservedFieldPattern(final Class<?> owner, final String resourceName) {
        final String location = owner.getName() + ": " + resourceName;
        try (InputStream input = owner.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Missing reserved-field resource: " + location);
            }
            final Properties properties = new Properties();
            properties.load(input);
            final String regex = properties.getProperty("reservedFieldPattern");
            if (regex == null || regex.trim().isEmpty()) {
                throw new IllegalStateException("Missing reservedFieldPattern in " + location);
            }
            return Pattern.compile(regex);
        } catch (final IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Invalid reserved-field resource: " + location, e);
        }
    }

    @Override
    public void close() {
        if (this.closed.compareAndSet(false, true)) {
            closeImpl();
        }
    }

    protected abstract void closeImpl();

    @Override
    public boolean isClosed() {
        return this.closed.get();
    }
}
