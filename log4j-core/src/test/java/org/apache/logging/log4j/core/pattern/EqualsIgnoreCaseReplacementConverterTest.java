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
package org.apache.logging.log4j.core.pattern;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;

/**
 *
 */
public class EqualsIgnoreCaseReplacementConverterTest {

    @Test
    public void testMarkerReplacement() {
        testReplacement("%marker", Strings.EMPTY);
    }

    @Test
    public void testMarkerSimpleNameReplacement() {
        testReplacement("%markerSimpleName", Strings.EMPTY);
    }

    @Test
    public void testLoggerNameReplacement() {
        testReplacement("%logger", "aaa[" + EqualsIgnoreCaseReplacementConverterTest.class.getName() + "]zzz");
    }

    private void testReplacement(final String tag, final String expectedValue) {
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(EqualsIgnoreCaseReplacementConverterTest.class.getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("This is a test")) //
                .build();
        final StringBuilder sb = new StringBuilder();
        final LoggerContext ctx = LoggerContext.getContext();
        final String[] options = new String[] { "aaa[" + tag + "]zzz", "AAA[]ZZZ", expectedValue };
        final EqualsIgnoreCaseReplacementConverter converter = EqualsIgnoreCaseReplacementConverter
                .newInstance(ctx.getConfiguration(), options);
        converter.format(event, sb);
        assertEquals(expectedValue, sb.toString());
    }
}
