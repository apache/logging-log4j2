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

import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MapMessage;
import org.junit.Test;

/**
 * Work in progress.
 */
public class CollectionLoggingTest {

    @Test
    public void testSystemProperties() {
        final Logger logger = LogManager.getLogger(CollectionLoggingTest.class.getName());
        logger.error(System.getProperties());
        // logger.error(new MapMessage(System.getProperties()));
    }

    @Test
    public void testSimpleMap() {
        final Logger logger = LogManager.getLogger(CollectionLoggingTest.class.getName());
        logger.error(System.getProperties());
        final Map<String, String> map = new HashMap<String, String>();
        map.put("MyKey1", "MyValue1");
        map.put("MyKey2", "MyValue2");
        logger.error(new MapMessage(map));
        logger.error(map);
    }

    @Test
    public void testNetworkInterfaces() throws SocketException {
        final Logger logger = LogManager.getLogger(CollectionLoggingTest.class.getName());
        logger.error(NetworkInterface.getNetworkInterfaces());
    }

    @Test
    public void testAvailableCharsets() throws SocketException {
        final Logger logger = LogManager.getLogger(CollectionLoggingTest.class.getName());
        logger.error(Charset.availableCharsets());
    }

}
