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
package org.apache.logging.log4j.tojul;

import java.net.URI;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Implementation of Log4j {@link LoggerContextFactory} SPI.
 * This is a factory to produce the (one and only) {@link JULLoggerContext} instance.
 *
 * @author <a href="http://www.vorburger.ch">Michael Vorburger.ch</a> for Google
 */
public class JULLoggerContextFactory implements LoggerContextFactory {
    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private static final LoggerContext context = new JULLoggerContext();

    // This implementation is strongly inspired by org.apache.logging.slf4j.SLF4JLoggerContextFactory

    public JULLoggerContextFactory() {
        boolean misconfigured = false;
        try {
            LoaderUtil.loadClass("org.apache.logging.log4j.jul.LogManager");
            misconfigured = true;
        } catch (final ClassNotFoundException classNotFoundIsGood) {
            LOGGER.debug("org.apache.logging.log4j.jul.LogManager is not on classpath. Good!");
        }
        if (misconfigured) {
            throw new IllegalStateException("log4j-jul JAR is mutually exclusive with the log4j-to-jul JAR"
                    + "(the first routes calls from Log4j to JUL, the second from Log4j to JUL)");
        }
    }

    @Override
    public LoggerContext getContext(
            final String fqcn, final ClassLoader loader, final Object externalContext, final boolean currentContext) {
        return context;
    }

    @Override
    public LoggerContext getContext(
            final String fqcn,
            final ClassLoader loader,
            final Object externalContext,
            final boolean currentContext,
            final URI configLocation,
            final String name) {
        return context;
    }

    @Override
    public void removeContext(final LoggerContext ignored) {}

    @Override
    public boolean isClassLoaderDependent() {
        // context is always used
        return false;
    }
}
