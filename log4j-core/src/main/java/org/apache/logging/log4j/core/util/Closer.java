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
package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.status.StatusLogger;

/**
 * Closes resources.
 */
public final class Closer {

    private Closer() {
        // empty
    }

    /**
     * Closes an AutoCloseable or ignores if {@code null}.
     *
     * @param closeable the resource to close; may be null
     * @return Whether the resource was closed.
     * @throws Exception if the resource cannot be closed
     * @since 2.8
     * @since 2.11.2 returns a boolean instead of being a void return type.
     */
    public static boolean close(final AutoCloseable closeable) throws Exception {
        if (closeable != null) {
            StatusLogger.getLogger().debug("Closing {} {}", closeable.getClass().getSimpleName(), closeable);
            closeable.close();
            return true;
        }
        return false;
    }

    /**
     * Closes an AutoCloseable and returns {@code true} if it closed without exception.
     *
     * @param closeable the resource to close; may be null
     * @return true if resource was closed successfully, or false if an exception was thrown
     */
    public static boolean closeSilently(final AutoCloseable closeable) {
        try {
            return close(closeable);
        } catch (final Exception ignored) {
            return false;
        }
    }
}
