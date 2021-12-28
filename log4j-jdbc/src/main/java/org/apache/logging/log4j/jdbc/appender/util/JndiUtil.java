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
package org.apache.logging.log4j.jdbc.appender.util;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.jdbc.appender.DataSourceConnectionSource;
import org.apache.logging.log4j.jndi.JndiManager;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * This class is solely for the purpose of keeping DataSourceConnectionSource from getting a NoClassDefFoundError.
 */
public final class JndiUtil {
    private static Logger LOGGER = StatusLogger.getLogger();

    private JndiUtil() {
    }

    public static DataSource getDataSource(String jndiName) {
        try {
            final DataSource dataSource =
                    JndiManager.getDefaultManager(DataSourceConnectionSource.class.getCanonicalName()).lookup(jndiName);
            if (dataSource == null) {
                LOGGER.error("No data source found with JNDI name [" + jndiName + "].");
                return null;
            }
            return dataSource;
        } catch (final NamingException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }
}
