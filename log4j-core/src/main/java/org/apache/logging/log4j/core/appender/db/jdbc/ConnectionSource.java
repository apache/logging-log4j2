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
package org.apache.logging.log4j.core.appender.db.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Configuration element for {@link JdbcAppender}. If you want to use the {@link JdbcAppender} but none of the provided
 * connection sources meet your needs, you can simply create your own connection source.
 */
public interface ConnectionSource {
    /**
     * This should return a new connection every time it is called.
     *
     * @return the SQL connection object.
     * @throws SQLException if a database error occurs.
     */
    Connection getConnection() throws SQLException;

    /**
     * All implementations must override {@link Object#toString()} to provide information about the connection
     * configuration (obscuring passwords with one-way hashes).
     *
     * @return the string representation of this connection source.
     */
    @Override
    String toString();
}
