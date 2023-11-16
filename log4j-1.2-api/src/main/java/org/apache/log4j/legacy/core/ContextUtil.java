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
package org.apache.log4j.legacy.core;

import org.apache.logging.log4j.spi.LoggerContext;

/**
 * Delegates to {@code LoggerContext} methods implemented by {@code log4j-core} if appropriate.
 */
public final class ContextUtil {

    /**
     * Delegates to {@link org.apache.logging.log4j.core.LoggerContext#reconfigure()} if appropriate.
     *
     * @param loggerContext The target logger context.
     */
    public static void reconfigure(final LoggerContext loggerContext) {
        if (loggerContext instanceof org.apache.logging.log4j.core.LoggerContext) {
            ((org.apache.logging.log4j.core.LoggerContext) loggerContext).reconfigure();
        }
    }

    /**
     * Delegates to {@link org.apache.logging.log4j.core.LoggerContext#close()} if appropriate.
     *
     * @param loggerContext The target logger context.
     */
    public static void shutdown(final LoggerContext loggerContext) {
        if (loggerContext instanceof org.apache.logging.log4j.core.LoggerContext) {
            ((org.apache.logging.log4j.core.LoggerContext) loggerContext).close();
        }
    }

    private ContextUtil() {}
}
