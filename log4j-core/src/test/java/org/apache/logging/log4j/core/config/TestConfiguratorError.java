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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.simple.SimpleLoggerContextFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class TestConfiguratorError {

    private static final String FACTORY_PROPERTY_NAME = "log4j2.loggerContextFactory";

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(FACTORY_PROPERTY_NAME, SimpleLoggerContextFactory.class.getName());
    }

    @Test
    public void testErrorNoClassLoader() throws Exception {
        try (final LoggerContext ctx = Configurator.initialize("Test1", "target/test-classes/log4j2-config.xml")) {
            assertNull("No LoggerContext should have been returned", ctx);
        }
    }

    @Test
    public void testErrorNullClassLoader() throws Exception {
        try (final LoggerContext ctx = Configurator.initialize("Test1", null,
                "target/test-classes/log4j2-config.xml")) {
            assertNull("No LoggerContext should have been returned", ctx);
        }
    }
}
