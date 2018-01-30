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
package org.apache.logging.log4j.jdbc.appender;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.jdbc.appender.AbstractConnectionSource;
import org.apache.logging.log4j.jdbc.appender.ConnectionSource;

public class JdbcH2TestHelper {

    public static final String CONNECTION_STRING = "jdbc:h2:mem:Log4j";
    public static final String USER_NAME = "sa";
    public static final String PASSWORD = "";
    
    public static ConnectionSource TEST_CONFIGURATION_SOURCE = new AbstractConnectionSource() {
        @Override
        public Connection getConnection() throws SQLException {
            return JdbcH2TestHelper.getConnection();
        }
    };

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONNECTION_STRING, USER_NAME, PASSWORD);
    }

}
