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
package org.apache.logging.log4j.simple;

import java.net.URI;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerContextFactory;

/**
 * Simple and stateless {@link LoggerContextFactory}.
 */
public class SimpleLoggerContextFactory implements LoggerContextFactory {

    /**
     * Singleton instance.
     */
    public static final SimpleLoggerContextFactory INSTANCE = new SimpleLoggerContextFactory();

    @Override
    public LoggerContext getContext(
            final String fqcn, final ClassLoader loader, final Object externalContext, final boolean currentContext) {
        return SimpleLoggerContext.INSTANCE;
    }

    @Override
    public LoggerContext getContext(
            final String fqcn,
            final ClassLoader loader,
            final Object externalContext,
            final boolean currentContext,
            final URI configLocation,
            final String name) {
        return SimpleLoggerContext.INSTANCE;
    }

    @Override
    public void removeContext(final LoggerContext removeContext) {
        // do nothing
    }

    @Override
    public boolean isClassLoaderDependent() {
        return false;
    }
}
