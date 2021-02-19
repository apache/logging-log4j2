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
package org.apache.log4j.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test configuration from Properties.
 */
public class PropertiesConfigurationFactoryTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.LOG4J1_CONFIGURATION_FILE_PROPERTY, "target/test-classes/log4j1-file.properties");
    }

    @Test
    public void testProperties() throws Exception {
        Logger logger = LogManager.getLogger("test");
        logger.debug("This is a test of the root logger");
        File file = new File("target/temp.A1");
        assertThat(file.exists()).describedAs("File A1 was not created").isTrue();
        assertThat(file.length()).describedAs("File A1 is empty").isGreaterThan(0);
        file = new File("target/temp.A2");
        assertThat(file.exists()).describedAs("File A2 was not created").isTrue();
        assertThat(file.length()).describedAs("File A2 is empty").isGreaterThan(0);
    }

}
