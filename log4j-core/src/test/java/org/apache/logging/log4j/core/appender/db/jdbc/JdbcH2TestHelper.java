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
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.lang3.SystemUtils;

public class JdbcH2TestHelper {

	/**
	 * A JDBC connection string for an H2 in-memory database.
	 */
	static final String CONNECTION_STRING_MEM = "jdbc:h2:mem:Log4j";

	/**
	 * A JDBC connection string for an H2 database in the Java temporary directory.
	 */
	static final String CONNECTION_STRING_TMPDIR = "jdbc:h2:" + SystemUtils.JAVA_IO_TMPDIR
			+ "/h2/test_log4j;TRACE_LEVEL_SYSTEM_OUT=0";

	static final String USER_NAME = "sa";
	static final String PASSWORD = "";

	public static ConnectionSource TEST_CONFIGURATION_SOURCE_MEM = new AbstractConnectionSource() {
		@Override
		public Connection getConnection() throws SQLException {
			return JdbcH2TestHelper.getConnectionMem();
		}
	};

	public static ConnectionSource TEST_CONFIGURATION_SOURCE_TMPDIR = new AbstractConnectionSource() {
		@Override
		public Connection getConnection() throws SQLException {
			return JdbcH2TestHelper.getConnectionTmpDir();
		}
	};

	public static Connection getConnectionMem() throws SQLException {
		return DriverManager.getConnection(CONNECTION_STRING_MEM, USER_NAME, PASSWORD);
	}

	public static Connection getConnectionTmpDir() throws SQLException {
		return DriverManager.getConnection(CONNECTION_STRING_TMPDIR, USER_NAME, PASSWORD);
	}

}
