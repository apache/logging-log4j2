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
package org.apache.logging.log4j.core.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class MapFilterTest {

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule("log4j2-mapfilter.xml");

    @Test
    public void testFilter() {
        final KeyValuePair[] pairs = new KeyValuePair[] { new KeyValuePair("FromAccount", "211000"),
                                                    new KeyValuePair("ToAccount", "123456")};
        MapFilter filter = MapFilter.createFilter(pairs, "and", null, null);
        filter.start();
        MapMessage msg = new MapMessage();
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "211000");
        msg.put("Amount", "1000.00");
        assertTrue(filter.isStarted());
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.DEBUG, null, msg, null));
        msg.put("ToAccount", "111111");
        assertSame(Filter.Result.DENY, filter.filter(null, Level.ERROR, null, msg, null));
        filter = MapFilter.createFilter(pairs, "or", null, null);
        filter.start();
        msg = new MapMessage();
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "211000");
        msg.put("Amount", "1000.00");
        assertTrue(filter.isStarted());
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.DEBUG, null, msg, null));
        msg.put("ToAccount", "111111");
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.ERROR, null, msg, null));
    }

    @Test
    public void testConfig() {
        final Configuration config = context.getConfiguration();
        final Filter filter = config.getFilter();
        assertNotNull("No MapFilter", filter);
        assertTrue("Not a MapFilter", filter instanceof  MapFilter);
        final MapFilter mapFilter = (MapFilter) filter;
        assertFalse("Should not be And filter", mapFilter.isAnd());
        final Map<String, List<String>> map = mapFilter.getMap();
        assertNotNull("No Map", map);
        assertFalse("No elements in Map", map.isEmpty());
        assertEquals("Incorrect number of elements in Map", 1, map.size());
        assertTrue("Map does not contain key eventId", map.containsKey("eventId"));
        assertEquals("List does not contain 2 elements", 2, map.get("eventId").size());
        final Logger logger = LogManager.getLogger(MapFilterTest.class);
        final Map<String, String> eventMap = new HashMap<>();
        eventMap.put("eventId", "Login");
        logger.debug(new MapMessage(eventMap));
        final ListAppender app = context.getListAppender("LIST");
        final List<String> msgs = app.getMessages();
        assertNotNull("No messages", msgs);
        assertFalse("No messages", msgs.isEmpty());


    }
}
