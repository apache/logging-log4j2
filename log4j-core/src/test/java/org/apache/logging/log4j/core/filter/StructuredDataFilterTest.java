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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 *
 */
public class StructuredDataFilterTest {

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule("log4j2-sdfilter.xml");

    @Test
    public void testFilter() {
        final KeyValuePair[] pairs = new KeyValuePair[] { new KeyValuePair("id.name", "AccountTransfer"),
                                                    new KeyValuePair("ToAccount", "123456")};
        StructuredDataFilter filter = StructuredDataFilter.createFilter(pairs, "and", null, null);
        filter.start();
        StructuredDataMessage msg = new StructuredDataMessage("AccountTransfer@18060", "Transfer Successful", "Audit");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "211000");
        msg.put("Amount", "1000.00");
        assertTrue(filter.isStarted());
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.DEBUG, null, msg, null));
        msg.put("ToAccount", "111111");
        assertSame(Filter.Result.DENY, filter.filter(null, Level.ERROR, null, msg, null));
        filter = StructuredDataFilter.createFilter(pairs, "or", null, null);
        filter.start();
        msg = new StructuredDataMessage("AccountTransfer@18060", "Transfer Successful", "Audit");
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
        assertNotNull("No StructuredDataFilter", filter);
        assertTrue("Not a StructuredDataFilter", filter instanceof  StructuredDataFilter);
        final StructuredDataFilter sdFilter = (StructuredDataFilter) filter;
        assertFalse("Should not be And filter", sdFilter.isAnd());
        final Map<String, List<String>> map = sdFilter.getMap();
        assertNotNull("No Map", map);
        assertFalse("No elements in Map", map.isEmpty());
        assertEquals("Incorrect number of elements in Map", 1, map.size());
        assertTrue("Map does not contain key eventId", map.containsKey("eventId"));
        assertEquals("List does not contain 2 elements", 2, map.get("eventId").size());
    }
}
