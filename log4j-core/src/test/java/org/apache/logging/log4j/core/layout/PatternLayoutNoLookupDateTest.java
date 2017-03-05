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
package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * See (LOG4J2-905) Ability to disable (date) lookup completely, compatibility issues with other libraries like camel.
 */
public class PatternLayoutNoLookupDateTest {

    @Rule
    public final LoggerContextRule context = new LoggerContextRule("log4j-list-nolookups.xml");

    @Test
    public void testDateLookupInMessage() {
        final String template = "${date:YYYY-MM-dd}";
        context.getLogger(PatternLayoutNoLookupDateTest.class.getName()).info(template);
        final ListAppender listAppender = context.getListAppender("List");
        final String string = listAppender.getMessages().get(0);
        Assert.assertTrue(string, string.contains(template));
    }

}
