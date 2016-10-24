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
package org.apache.logging.log4j;

import org.apache.logging.log4j.lookup.CustomLookup;
import org.apache.logging.log4j.lookup.CustomMapMessage;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rgoers on 8/2/15.
 */
public class CustomPropertiesTest {

    @Test
    public void testProperties() throws Exception {
        final Logger logger = LogManager.getLogger("TestProperties");
        final Map<String, String> loggerProperties = new ConcurrentHashMap<>();
        CustomLookup.setLoggerProperties("TestProperties", loggerProperties);
        loggerProperties.put("key1", "CustomPropertiesTest");
        loggerProperties.put("key2", "TestValue");
        logger.debug("This is a test");
    }

    @Test
    public void mapMessageProperties() throws Exception {
        final Logger logger = LogManager.getLogger("MapProperties");
        final Map<String, String> loggerProperties = new ConcurrentHashMap<>();
        loggerProperties.put("key1", "CustomPropertiesTest");
        loggerProperties.put("key2", "TestValue");
        logger.debug(new CustomMapMessage("This is a test", loggerProperties));
    }


}
