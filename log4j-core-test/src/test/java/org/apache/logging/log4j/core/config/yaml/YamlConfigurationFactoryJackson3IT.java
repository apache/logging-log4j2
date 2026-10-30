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
package org.apache.logging.log4j.core.config.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.Test;

class YamlConfigurationFactoryJackson3IT {

    @Test
    void usesJackson3WhenPresent() throws Exception {
        final ConfigurationSource source = ConfigurationSource.fromUri(
                getClass().getResource("/log4j-jackson3.yaml").toURI());
        final Configuration config = new YamlConfigurationFactory().getConfiguration(null, source);
        assertThat(config).isInstanceOf(Jackson3YamlConfiguration.class);
        config.initialize();
        assertThat(config.getName()).isEqualTo("Jackson3Test");
        final Appender appender = config.getAppender("LIST");
        assertThat(appender).isInstanceOf(ListAppender.class);
        final LoggerConfig logger = config.getLoggerConfig("org.example");
        assertThat(logger.getName()).isEqualTo("org.example");
        assertThat(logger.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(logger.isAdditive()).isFalse();
        assertThat(logger.getAppenderRefs()).extracting(AppenderRef::getRef).containsExactly("LIST");
        assertThat(config.getRootLogger().getLevel()).isEqualTo(Level.ERROR);
        assertThat(((Reconfigurable) config).reconfigure()).isInstanceOf(Jackson3YamlConfiguration.class);
    }
}
