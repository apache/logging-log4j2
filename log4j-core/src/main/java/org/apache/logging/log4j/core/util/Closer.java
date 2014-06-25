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

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Helper class for closing resources.
 */
public final class Closer {

    private Closer() {
    }

    /**
     * Closes the specified {@code Closeable} (stream or reader/writer),
     * ignoring any exceptions thrown by the close operation.
     *
     * @param closeable the resource to close, may be {@code null}
     */
    public static void closeSilently(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final Exception ignored) {
            // ignored
        }
    }

    /**
     * Closes the specified {@code Closeable} (stream or reader/writer).
     *
     * @param closeable the resource to close, may be {@code null}
     * @throws IOException if a problem occurred closing the specified resource
     */
    public static void close(final Closeable closeable) throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }

    /**
     * Closes the specified resource, ignoring any exceptions thrown by the close operation.
     *
     * @param serverSocket the resource to close, may be {@code null}
     */
    public static void closeSilently(final ServerSocket serverSocket) {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (final Exception ignored) {
            // ignored
        }
    }

    /**
     * Closes the specified resource.
     *
     * @param serverSocket the resource to close, may be {@code null}
     * @throws IOException if a problem occurred closing the specified resource
     */
    public static void close(final ServerSocket serverSocket) throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    /**
     * Closes the specified resource, ignoring any exceptions thrown by the close operation.
     *
     * @param datagramSocket the resource to close, may be {@code null}
     */
    public static void closeSilently(final DatagramSocket datagramSocket) {
        try {
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        } catch (final Exception ignored) {
            // ignored
        }
    }

    /**
     * Closes the specified resource.
     *
     * @param datagramSocket the resource to close, may be {@code null}
     * @throws IOException if a problem occurred closing the specified resource
     */
    public static void close(final DatagramSocket datagramSocket) throws IOException {
        if (datagramSocket != null) {
            datagramSocket.close();
        }
    }

    /**
     * Closes the specified {@code Statement}, ignoring any exceptions thrown by
     * the close operation.
     *
     * @param statement the resource to close, may be {@code null}
     */
    public static void closeSilently(final Statement statement) {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (final Exception ignored) {
            // ignored
        }
    }

    /**
     * Closes the specified {@code Statement}.
     *
     * @param statement the resource to close, may be {@code null}
     * @throws SQLException if a problem occurred closing the specified resource
     */
    public static void close(final Statement statement) throws SQLException {
        if (statement != null) {
            statement.close();
        }
    }

    /**
     * Closes the specified {@code Connection}, ignoring any exceptions thrown
     * by the close operation.
     *
     * @param connection the resource to close, may be {@code null}
     */
    public static void closeSilently(final Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (final Exception ignored) {
            // ignored
        }
    }

    /**
     * Closes the specified {@code Connection}.
     *
     * @param connection the resource to close, may be {@code null}
     * @throws SQLException if a problem occurred closing the specified resource
     */
    public static void close(final Connection connection) throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Closes the specified {@code Context}, ignoring any exceptions thrown by the close operation.
     *
     * @param context the JNDI Context to close, may be {@code null}
     */
    public static void closeSilently(final Context context) {
        try {
            close(context);
        } catch (final NamingException ignored) {
            // ignored
        }
    }

    /**
     * Closes the specified {@code Context}.
     *
     * @param context the JNDI Context to close, may be {@code null}
     * @throws NamingException if a problem occurred closing the specified JNDI Context
     */
    public static void close(final Context context) throws NamingException {
        if (context != null) {
            context.close();
        }
    }

}
