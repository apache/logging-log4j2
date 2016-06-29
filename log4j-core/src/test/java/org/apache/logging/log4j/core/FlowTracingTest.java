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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.message.EntryMessage;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Incomplete test to illustrate LOG4J2-1452.
 */
public class FlowTracingTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "FlowTracingTest.xml");
    }

    @Test
    public void testFlowTracing() throws Exception {
        final org.apache.logging.log4j.Logger logger = LogManager.getLogger("com.foo.Bar");

        EntryMessage entryMessage = logger.traceEntry(logger.getMessageFactory().newMessage("the entry"));
        logger.info("something else");
        logger.traceExit(entryMessage);
    }

}
