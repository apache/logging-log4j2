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
package org.apache.log4j.config;

import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test configuration from XML.
 */
public class XmlConfigurationFactoryTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(
                ConfigurationFactory.LOG4J1_CONFIGURATION_FILE_PROPERTY, "target/test-classes/log4j1-file.xml");
    }

    @Test
    public void testXML() {
        final Logger logger = LogManager.getLogger("test");
        logger.debug("This is a test of the root logger");
        File file = new File("target/temp.A1");
        assertTrue("File A1 was not created", file.exists());
        assertTrue("File A1 is empty", file.length() > 0);
        file = new File("target/temp.A2");
        assertTrue("File A2 was not created", file.exists());
        assertTrue("File A2 is empty", file.length() > 0);
    }
}
