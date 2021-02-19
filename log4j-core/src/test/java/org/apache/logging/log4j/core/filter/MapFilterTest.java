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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.junit.jupiter.api.Test;

public class MapFilterTest {

    @Test
    public void testFilter() {
        final KeyValuePair[] pairs = new KeyValuePair[] { new KeyValuePair("FromAccount", "211000"),
                                                    new KeyValuePair("ToAccount", "123456")};
        MapFilter filter = MapFilter.createFilter(pairs, "and", null, null);
        assertThat(filter).isNotNull();
        filter.start();
        StringMapMessage msg = new StringMapMessage();
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "211000");
        msg.put("Amount", "1000.00");
        assertThat(filter.isStarted()).isTrue();
        assertThat(filter.filter(null, Level.DEBUG, null, msg, null)).isSameAs(Filter.Result.NEUTRAL);
        msg.put("ToAccount", "111111");
        assertThat(filter.filter(null, Level.ERROR, null, msg, null)).isSameAs(Filter.Result.DENY);
        filter = MapFilter.createFilter(pairs, "or", null, null);
        assertThat(filter).isNotNull();
        filter.start();
        msg = new StringMapMessage();
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "211000");
        msg.put("Amount", "1000.00");
        assertThat(filter.isStarted()).isTrue();
        assertThat(filter.filter(null, Level.DEBUG, null, msg, null)).isSameAs(Filter.Result.NEUTRAL);
        msg.put("ToAccount", "111111");
        assertThat(filter.filter(null, Level.ERROR, null, msg, null)).isSameAs(Filter.Result.NEUTRAL);
    }

    @Test
    @LoggerContextSource("log4j2-mapfilter.xml")
    public void testConfig(final Configuration config, @Named("LIST") final ListAppender app) {
        final Filter filter = config.getFilter();
        assertThat(filter).describedAs("No MapFilter").isNotNull();
        assertTrue(filter instanceof  MapFilter, "Not a MapFilter");
        final MapFilter mapFilter = (MapFilter) filter;
        assertFalse(mapFilter.isAnd(), "Should not be And filter");
        final IndexedReadOnlyStringMap map = mapFilter.getStringMap();
        assertThat(map).describedAs("No Map").isNotNull();
        assertFalse(map.isEmpty(), "No elements in Map");
        assertThat(map.size()).describedAs("Incorrect number of elements in Map").isEqualTo(1);
        assertTrue(map.containsKey("eventId"), "Map does not contain key eventId");
        assertThat(map.<Collection<?>>getValue("eventId").size()).describedAs("List does not contain 2 elements").isEqualTo(2);
        final Logger logger = LogManager.getLogger(MapFilterTest.class);
        final Map<String, String> eventMap = new HashMap<>();
        eventMap.put("eventId", "Login");
        logger.debug(new StringMapMessage(eventMap));
        final List<String> msgs = app.getMessages();
        assertThat(msgs).describedAs("No messages").isNotNull();
        assertFalse(msgs.isEmpty(), "No messages");
    }
}
