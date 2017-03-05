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

package org.apache.logging.log4j.core.util;

/**
 * Helper class for closing resources.
 */
public final class Closer {

    private Closer() {
    }

    /**
     * Closes an AutoCloseable or ignores if {@code null}.
     *
     * @param closeable the resource to close; may be null
     * @throws Exception if the resource cannot be closed
     * @since 2.8
     */
    public static void close(final AutoCloseable closeable) throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    /**
     * Closes an AutoCloseable and returns {@code true} if it closed without exception.
     *
     * @param closeable the resource to close; may be null
     * @return true if resource was closed successfully, or false if an exception was thrown
     */
    public static boolean closeSilently(final AutoCloseable closeable) {
        try {
            close(closeable);
            return true;
        } catch (final Exception ignored) {
            return false;
        }
    }

}
