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

import java.io.Serializable;
import java.util.List;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.layout.AbstractStringLayout.Serializer;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@LoggerContextSource("LOG4J-2195/log4j2.xml")
public class Log4j2_2195_Test {

    @Test
    public void test(final LoggerContext context, @Named("ListAppender") final ListAppender listAppender) {
        listAppender.clear();
        context.getLogger(getClass()).info("This is a test.", new Exception("Test exception!"));
        assertNotNull(listAppender);
        List<String> events = listAppender.getMessages();
        assertNotNull(events);
        assertEquals(1, events.size());
        String logEvent = events.get(0);
        assertNotNull(logEvent);
        assertFalse(logEvent.contains("org.junit"), "\"org.junit\" should not be here");
        assertFalse(logEvent.contains("org.eclipse"), "\"org.eclipse\" should not be here");
        //
        Layout<? extends Serializable> layout = listAppender.getLayout();
        PatternLayout pLayout = (PatternLayout) layout;
        assertNotNull(pLayout);
        Serializer eventSerializer = pLayout.getEventSerializer();
        assertNotNull(eventSerializer);
        //
        assertTrue(logEvent.contains("|"), "Missing \"|\"");
    }
}
