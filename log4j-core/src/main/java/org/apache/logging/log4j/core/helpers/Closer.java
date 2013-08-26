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

package org.apache.logging.log4j.core.helpers;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Helper class for closing resources.
 */
public class Closer {

    /**
     * Closes the specified {@code Closeable} (stream or reader/writer),
     * ignoring any exceptions thrown by the close operation.
     * 
     * @param closeable the resource to close, may be {@code null}
     */
    public static void closeSilent(Closeable closeable) {
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
    public static void close(Closeable closeable) throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }

    /**
     * Closes the specified {@code Statement}, ignoring any exceptions thrown by
     * the close operation.
     * 
     * @param statement the resource to close, may be {@code null}
     */
    public static void closeSilent(Statement statement) {
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
    public static void close(Statement statement) throws SQLException {
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
    public static void closeSilent(Connection connection) {
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
    public static void close(Connection connection) throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

}
