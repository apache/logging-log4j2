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
package org.apache.logging.log4j.core;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.message.StringMapMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@LoggerContextSource("log4j-collectionLogging.xml")
@Disabled("Work in progress")
public class CollectionLoggingTest {

    private final ListAppender app;

    public CollectionLoggingTest(@Named("List") final ListAppender app) {
        this.app = app.clear();
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    public void testSystemProperties(final LoggerContext context) {
        final Logger logger = context.getLogger(CollectionLoggingTest.class.getName());
        logger.error(System.getProperties());
        // logger.error(new MapMessage(System.getProperties()));
        // TODO: some assertions
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    public void testSimpleMap(final LoggerContext context) {
        final Logger logger = context.getLogger(CollectionLoggingTest.class.getName());
        logger.error(System.getProperties());
        final Map<String, String> map = new HashMap<>();
        map.put("MyKey1", "MyValue1");
        map.put("MyKey2", "MyValue2");
        logger.error(new StringMapMessage(map));
        logger.error(map);
        // TODO: some assertions
    }

    @Test
    public void testNetworkInterfaces(final LoggerContext context) throws SocketException {
        final Logger logger = context.getLogger(CollectionLoggingTest.class.getName());
        logger.error(NetworkInterface.getNetworkInterfaces());
        // TODO: some assertions
    }

    @Test
    public void testAvailableCharsets(final LoggerContext context) {
        final Logger logger = context.getLogger(CollectionLoggingTest.class.getName());
        logger.error(Charset.availableCharsets());
        // TODO: some assertions
    }
}
