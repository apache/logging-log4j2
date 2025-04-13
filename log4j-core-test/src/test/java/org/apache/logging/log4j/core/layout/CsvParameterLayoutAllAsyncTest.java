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
package org.apache.logging.log4j.core.layout;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link AbstractCsvLayout} with all loggers async.
 *
 * @since 2.6
 */
@Tag("Layouts.Csv")
class CsvParameterLayoutAllAsyncTest {

    @BeforeAll
    static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, AsyncLoggerContextSelector.class.getName());
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "AsyncLoggerTest.xml");
    }

    @AfterAll
    static void afterClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, Strings.EMPTY);
    }

    @Test
    void testLayoutDefaultNormal() throws Exception {
        final Logger root = (Logger) LogManager.getRootLogger();
        CsvParameterLayoutTest.testLayoutNormalApi(root, CsvParameterLayout.createDefaultLayout(), false);
    }

    @Test
    void testLayoutDefaultObjectArrayMessage() throws Exception {
        final Logger root = (Logger) LogManager.getRootLogger();
        CsvParameterLayoutTest.testLayoutNormalApi(root, CsvParameterLayout.createDefaultLayout(), true);
    }

    @Test
    void testLayoutTab() throws Exception {
        final Logger root = (Logger) LogManager.getRootLogger();
        CsvParameterLayoutTest.testLayoutNormalApi(root, CsvParameterLayout.createLayout(CSVFormat.TDF), true);
    }
}
