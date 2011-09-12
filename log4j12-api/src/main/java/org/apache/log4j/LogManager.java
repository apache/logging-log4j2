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
package org.apache.log4j;

import org.apache.logging.log4j.spi.LoggerContext;

/**
 *
 */
public final class LogManager {

    private LogManager() {
    }

    public static Logger getRootLogger() {
        return (Logger) PrivateManager.getLogger("");
    }

    public static Logger getLogger(final String name) {
        return (Logger) PrivateManager.getLogger(name);
    }

    public static Logger getLogger(final Class clazz) {
        return (Logger) PrivateManager.getLogger(clazz.getName());
    }

    public static Logger exists(String name) {
        LoggerContext ctx = PrivateManager.getContext();
        if (!ctx.hasLogger(name)) {
            return null;
        }
        return Logger.getLogger(name);
    }

    /**
     * Internal LogManager.
     */
    private static class PrivateManager extends org.apache.logging.log4j.LogManager {
        private static final String FQCN = LogManager.class.getName();

        public static LoggerContext getContext() {
            return getContext(FQCN, false);
        }

        public static org.apache.logging.log4j.Logger getLogger(String name) {
            return getLogger(FQCN, name);
        }
    }
}
