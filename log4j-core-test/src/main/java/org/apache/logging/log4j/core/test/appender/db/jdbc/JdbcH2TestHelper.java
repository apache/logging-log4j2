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
package org.apache.logging.log4j.core.test.appender.db.jdbc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.logging.log4j.core.appender.db.jdbc.AbstractConnectionSource;
import org.apache.logging.log4j.core.appender.db.jdbc.ConnectionSource;

public class JdbcH2TestHelper {

    /**
     * A JDBC connection string for an H2 in-memory database.
     */
    public static final String CONNECTION_STRING_IN_MEMORY = "jdbc:h2:mem:Log4j";

    /**
     * A JDBC connection string for a permanent H2 database.
     *
     * Since 2.22.0 this uses a permanent in-memory database.
     */
    @Deprecated
    public static final String CONNECTION_STRING_TEMP_DIR = "jdbc:h2:mem:Log4j_perm;DB_CLOSE_DELAY=-1";

    public static final String USER_NAME = "sa";
    public static final String PASSWORD = "";

    public static ConnectionSource TEST_CONFIGURATION_SOURCE_MEM = new AbstractConnectionSource() {
        @Override
        public Connection getConnection() throws SQLException {
            return JdbcH2TestHelper.getConnectionInMemory();
        }
    };

    @Deprecated
    public static ConnectionSource TEST_CONFIGURATION_SOURCE_TMPDIR = new AbstractConnectionSource() {
        @Override
        public Connection getConnection() throws SQLException {
            return JdbcH2TestHelper.getConnectionTempDir();
        }
    };

    @Deprecated
    public static void deleteDir() throws IOException {
        // Since 2.22.0 this is a no-op
    }

    @SuppressFBWarnings(value = "DMI_EMPTY_DB_PASSWORD")
    public static Connection getConnectionInMemory() throws SQLException {
        return DriverManager.getConnection(CONNECTION_STRING_IN_MEMORY, USER_NAME, PASSWORD);
    }

    /**
     * Since 2.22.0 this uses a permanent in-memory database.
     */
    @Deprecated
    @SuppressFBWarnings(value = "DMI_EMPTY_DB_PASSWORD")
    public static Connection getConnectionTempDir() throws SQLException {
        return DriverManager.getConnection(CONNECTION_STRING_TEMP_DIR, USER_NAME, PASSWORD);
    }
}
