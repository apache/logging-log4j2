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
package org.apache.logging.log4j.jdbc.appender;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class JdbcH2TestHelper {

    /**
     * A JDBC connection string for an H2 in-memory database.
     */
    static final String CONNECTION_STRING_IN_MEMORY = "jdbc:h2:mem:Log4j";

    /**
     * A JDBC connection string for a permanent H2 database.
     */
    private static final String CONNECTION_STRING_IN_MEMORY_PERMANENT = "jdbc:h2:mem:Log4j_perm;DB_CLOSE_DELAY=-1";

    public static final String USER_NAME = "sa";
    public static final String PASSWORD = "";

    public static ConnectionSource TEST_CONFIGURATION_SOURCE_MEM = new AbstractConnectionSource() {
        @Override
        public Connection getConnection() throws SQLException {
            return JdbcH2TestHelper.getConnectionInMemory();
        }
    };

    public static ConnectionSource TEST_CONFIGURATION_SOURCE_MEM_PERM = new AbstractConnectionSource() {
        @Override
        public Connection getConnection() throws SQLException {
            return JdbcH2TestHelper.getConnectionInMemoryPermanent();
        }
    };

    static Connection getConnectionInMemory() throws SQLException {
        return DriverManager.getConnection(CONNECTION_STRING_IN_MEMORY, USER_NAME, PASSWORD);
    }

    @SuppressFBWarnings(value = "DMI_EMPTY_DB_PASSWORD")
    static Connection getConnectionInMemoryPermanent() throws SQLException {
        return DriverManager.getConnection(CONNECTION_STRING_IN_MEMORY_PERMANENT, USER_NAME, PASSWORD);
    }

}
