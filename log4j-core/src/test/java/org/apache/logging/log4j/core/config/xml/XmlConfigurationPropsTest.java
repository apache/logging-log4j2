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
package org.apache.logging.log4j.core.config.xml;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class XmlConfigurationPropsTest {

    private static final String CONFIG = "log4j-props.xml";
    private static final String CONFIG1 = "log4j-props1.xml";

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testNoProps() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
        final Configuration config = ctx.getConfiguration();
        assertTrue("Configuration is not an XmlConfiguration", config instanceof XmlConfiguration);
    }


    @Test
    public void testDefaultStatus() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG1);
        System.setProperty(Constants.LOG4J_DEFAULT_STATUS_LEVEL, "WARN");
        try {
            final LoggerContext ctx = LoggerContext.getContext();
            ctx.reconfigure();
            final Configuration config = ctx.getConfiguration();
            assertTrue("Configuration is not an XmlConfiguration", config instanceof XmlConfiguration);
        } finally {
            System.clearProperty(Constants.LOG4J_DEFAULT_STATUS_LEVEL);
        }
    }

    @Test
    public void testWithConfigProp() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        System.setProperty("log4j.level", "warn");
        try {
            final LoggerContext ctx = LoggerContext.getContext();
            ctx.reconfigure();
            final Configuration config = ctx.getConfiguration();
            assertTrue("Configuration is not an XmlConfiguration", config instanceof XmlConfiguration);
        } finally {
            System.clearProperty("log4j.level");

        }
    }

    @Test
    public void testWithProps() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        System.setProperty("log4j.level", "warn");
        System.setProperty("log.level", "warn");
        try {
            final LoggerContext ctx = LoggerContext.getContext();
            ctx.reconfigure();
            final Configuration config = ctx.getConfiguration();
            assertTrue("Configuration is not an XmlConfiguration", config instanceof XmlConfiguration);
        } finally {
            System.clearProperty("log4j.level");
            System.clearProperty("log.level");
        }
    }
}
