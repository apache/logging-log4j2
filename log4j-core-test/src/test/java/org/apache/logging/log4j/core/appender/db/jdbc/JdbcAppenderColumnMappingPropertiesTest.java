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
package org.apache.logging.log4j.core.appender.db.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.util.Date;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.db.ColumnMapping;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

@UsingStatusListener
public class JdbcAppenderColumnMappingPropertiesTest {

    public Connection getConnection() {
        return null;
    }

    /**
     * Tests the possibility to configure {@link org.apache.logging.log4j.core.appender.db.ColumnMapping} in a
     * properties configuration.
     * @see <a href="https://github.com/apache/logging-log4j2/issues/1405">#1405</a>
     */
    @Test
    void testColumnMapping() throws URISyntaxException {
        final URL configLocation = JdbcAppenderColumnMappingPropertiesTest.class.getResource(
                "JdbcAppenderColumnMappingPropertiesTest.properties");
        assertThat(configLocation).isNotNull();
        final Configuration config =
                PropertiesConfigurationFactory.getInstance().getConfiguration(null, null, configLocation.toURI());
        assertThat(config).isNotNull();
        config.initialize();
        final Appender appender = config.getAppender("Jdbc");
        assertThat(appender).isInstanceOf(JdbcAppender.class);
        final JdbcAppender jdbcAppender = (JdbcAppender) appender;

        final ColumnMapping expected = ColumnMapping.newBuilder()
                .setName("timestamp")
                .setColumnType(Date.class)
                .build();
        final ColumnMapping[] mappings = jdbcAppender.getManager().factoryData.columnMappings;
        assertThat(mappings).hasSize(1);
        assertThat(mappings[0]).isEqualTo(expected);
    }
}
