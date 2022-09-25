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

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * See (LOG4J2-905) Ability to disable (date) lookup completely, compatibility issues with other libraries like camel.
 */
@LoggerContextSource("log4j-list.xml")
public class PatternLayoutNoLookupDateTest {

    @Test
    public void testDateLookupInMessage(final LoggerContext context, @Named("List") final ListAppender listAppender) {
        listAppender.clear();
        final String template = "${date:YYYY-MM-dd}";
        context.getLogger(PatternLayoutNoLookupDateTest.class).info(template);
        final String string = listAppender.getMessages().get(0);
        Assertions.assertTrue(string.contains(template), string);
    }

}
