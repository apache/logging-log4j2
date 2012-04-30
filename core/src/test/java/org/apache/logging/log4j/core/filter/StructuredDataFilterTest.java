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
import org.apache.logging.log4j.core.helpers.KeyValuePair;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class StructuredDataFilterTest {


    @Test
    public void testFilter() {
        KeyValuePair[] pairs = new KeyValuePair[] { new KeyValuePair("id.name", "AccountTransfer"),
                                                    new KeyValuePair("ToAccount", "123456")};
        StructuredDataFilter filter = StructuredDataFilter.createFilter(pairs, "and", null, null);
        filter.start();
        StructuredDataMessage msg = new StructuredDataMessage("AccountTransfer@18060", "Transfer Successful", "Audit");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "211000");
        msg.put("Amount", "1000.00");
        assertTrue(filter.isStarted());
        assertTrue(filter.filter(null, Level.DEBUG, null, msg, (Throwable)null) == Filter.Result.NEUTRAL);
        msg.put("ToAccount", "111111");
        assertTrue(filter.filter(null, Level.ERROR, null, msg, (Throwable)null) == Filter.Result.DENY);
        filter = StructuredDataFilter.createFilter(pairs, "or", null, null);
        filter.start();
        msg = new StructuredDataMessage("AccountTransfer@18060", "Transfer Successful", "Audit");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "211000");
        msg.put("Amount", "1000.00");
        assertTrue(filter.isStarted());
        assertTrue(filter.filter(null, Level.DEBUG, null, msg, (Throwable)null) == Filter.Result.NEUTRAL);
        msg.put("ToAccount", "111111");
        assertTrue(filter.filter(null, Level.ERROR, null, msg, (Throwable)null) == Filter.Result.NEUTRAL);
    }
}
