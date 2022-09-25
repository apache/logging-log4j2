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
package org.apache.logging.log4j.core.test.appender.db.jdbc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.core.appender.db.jdbc.AbstractConnectionSource;
import org.apache.logging.log4j.core.appender.db.jdbc.ConnectionSource;

public class JdbcH2TestHelper {

    /**
     * A JDBC connection string for an H2 in-memory database.
     */
    public static final String CONNECTION_STRING_IN_MEMORY = "jdbc:h2:mem:Log4j";

    /**
     * A JDBC connection string for an H2 database in the Java temporary directory.
     */
    public static final String CONNECTION_STRING_TEMP_DIR = "jdbc:h2:" + getH2Path() + "/test_log4j;TRACE_LEVEL_SYSTEM_OUT=0";

    public static final String USER_NAME = "sa";
    public static final String PASSWORD = "";

    public static ConnectionSource TEST_CONFIGURATION_SOURCE_MEM = new AbstractConnectionSource() {
        @Override
        public Connection getConnection() throws SQLException {
            return JdbcH2TestHelper.getConnectionInMemory();
        }
    };

    public static ConnectionSource TEST_CONFIGURATION_SOURCE_TMPDIR = new AbstractConnectionSource() {
        @Override
        public Connection getConnection() throws SQLException {
            return JdbcH2TestHelper.getConnectionTempDir();
        }
    };

    /** Directory used in configuration files and connection strings. */
    static final String H2_TEST_RELATIVE_DIR = "h2";

    public static void deleteDir() throws IOException {
        final Path resolve = getH2Path();
        if (Files.exists(resolve)) {
            PathUtils.deleteDirectory(resolve);
        }
    }

    public static Connection getConnectionInMemory() throws SQLException {
        return DriverManager.getConnection(CONNECTION_STRING_IN_MEMORY, USER_NAME, PASSWORD);
    }

    public static Connection getConnectionTempDir() throws SQLException {
        return DriverManager.getConnection(CONNECTION_STRING_TEMP_DIR, USER_NAME, PASSWORD);
    }

    private static Path getH2Path() {
        return SystemUtils.getJavaIoTmpDir().toPath().resolve(H2_TEST_RELATIVE_DIR).normalize();
    }

}
