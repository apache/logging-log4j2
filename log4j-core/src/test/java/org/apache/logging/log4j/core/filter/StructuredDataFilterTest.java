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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.junit.jupiter.api.Test;

public class StructuredDataFilterTest {

    @Test
    public void testFilter() {
        final KeyValuePair[] pairs = new KeyValuePair[] { new KeyValuePair("id.name", "AccountTransfer"),
                                                    new KeyValuePair("ToAccount", "123456")};
        StructuredDataFilter filter = StructuredDataFilter.createFilter(pairs, "and", null, null);
        assertThat(filter).isNotNull();
        filter.start();
        StructuredDataMessage msg = new StructuredDataMessage("AccountTransfer@18060", "Transfer Successful", "Audit");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "211000");
        msg.put("Amount", "1000.00");
        assertThat(filter.isStarted()).isTrue();
        assertThat(filter.filter(null, Level.DEBUG, null, msg, null)).isSameAs(Filter.Result.NEUTRAL);
        msg.put("ToAccount", "111111");
        assertThat(filter.filter(null, Level.ERROR, null, msg, null)).isSameAs(Filter.Result.DENY);
        filter = StructuredDataFilter.createFilter(pairs, "or", null, null);
        assertThat(filter).isNotNull();
        filter.start();
        msg = new StructuredDataMessage("AccountTransfer@18060", "Transfer Successful", "Audit");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "211000");
        msg.put("Amount", "1000.00");
        assertThat(filter.isStarted()).isTrue();
        assertThat(filter.filter(null, Level.DEBUG, null, msg, null)).isSameAs(Filter.Result.NEUTRAL);
        msg.put("ToAccount", "111111");
        assertThat(filter.filter(null, Level.ERROR, null, msg, null)).isSameAs(Filter.Result.NEUTRAL);
    }

    @Test
    @LoggerContextSource("log4j2-sdfilter.xml")
    public void testConfig(final Configuration config) {
        final Filter filter = config.getFilter();
        assertThat(filter).describedAs("No StructuredDataFilter").isNotNull();
        assertTrue(filter instanceof  StructuredDataFilter, "Not a StructuredDataFilter");
        final StructuredDataFilter sdFilter = (StructuredDataFilter) filter;
        assertFalse(sdFilter.isAnd(), "Should not be And filter");
        final IndexedReadOnlyStringMap map = sdFilter.getStringMap();
        assertThat(map).describedAs("No Map").isNotNull();
        assertFalse(map.isEmpty(), "No elements in Map");
        assertThat(map.size()).describedAs("Incorrect number of elements in Map").isEqualTo(1);
        assertTrue(map.containsKey("eventId"), "Map does not contain key eventId");
        assertThat(map.<Collection<?>>getValue("eventId")).describedAs("List does not contain 2 elements").hasSize(2);
    }
}
