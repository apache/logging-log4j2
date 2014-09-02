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
package org.apache.logging.log4j.jdk;

import java.util.logging.Logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.AbstractExternalLoggerContextRegistry;
import org.apache.logging.log4j.spi.LoggerContext;

/**
 * {@link Logger} registry implementation.
 *
 * @since 2.1
 */
public class LoggerRegistry extends AbstractExternalLoggerContextRegistry<Logger> {

    @Override
    public Logger newLogger(final String name, final LoggerContext context) {
        return new org.apache.logging.log4j.jdk.Logger((org.apache.logging.log4j.core.Logger) context.getLogger(name));
    }

    @Override
    public LoggerContext getContext() {
        return PrivateManager.getContext();
    }

    private static class PrivateManager extends LogManager {
        private static final String FQCN = java.util.logging.LogManager.class.getName();

        public static LoggerContext getContext() {
            return getContext(FQCN, false);
        }
    }

}
