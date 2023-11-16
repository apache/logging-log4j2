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
package org.apache.log4j.defaultInit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Enumeration;
import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.config.TestConfigurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class TestCase3 {

    @AfterAll
    public static void tearDown() {
        LogManager.shutdown();
    }

    @Test
    public void propertiesTest() throws IOException {
        TestConfigurator.configure("src/test/resources/log4j1-1.2.17/input/defaultInit3.properties");
        final Logger root = Logger.getRootLogger();
        final boolean rootIsConfigured = root.getAllAppenders().hasMoreElements();
        assertTrue(rootIsConfigured);
        final Enumeration e = root.getAllAppenders();
        final Appender appender = (Appender) e.nextElement();
        assertEquals(appender.getName(), "D3");
    }
}
