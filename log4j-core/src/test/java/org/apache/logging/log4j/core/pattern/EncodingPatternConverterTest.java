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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class EncodingPatternConverterTest {

    @Test
    public void testReplacement() {
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(EncodingPatternConverterTest.class.getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("Test \r\n<div class=\"test\">this</div> & <div class='test'>that</div>"))
                .build();
        final StringBuilder sb = new StringBuilder();
        final LoggerContext ctx = LoggerContext.getContext();
        final String[] options = new String[]{"%msg"};
        final EncodingPatternConverter converter = EncodingPatternConverter
            .newInstance(ctx.getConfiguration(), options);
        assertNotNull("Error creating converter", converter);
        converter.format(event, sb);
        assertEquals(
            "Test \\r\\n&lt;div class=&quot;test&quot;&gt;this&lt;&#x2F;div&gt; &amp; &lt;div class=&apos;test&apos;&gt;that&lt;&#x2F;div&gt;",
            sb.toString());
    }

    @Test
    public void testJsonEscaping() throws Exception {
        final LogEvent event = Log4jLogEvent.newBuilder()
            .setLoggerName(getClass().getName())
            .setLevel(Level.DEBUG)
            .setMessage(new SimpleMessage("This string contains \"quotes\" and \\ backslash and \u001F control"))
            .build();
        final String expected = "This string contains \\\"quotes\\\" and \\\\ backslash and \\u001F control";
        final StringBuilder sb = new StringBuilder();
        final LoggerContext ctx = LoggerContext.getContext();
        final String[] options = new String[]{"%msg", "JSON"};
        final EncodingPatternConverter converter = EncodingPatternConverter.newInstance(ctx.getConfiguration(), options);

        assertNotNull("Error creating converter", converter);
        converter.format(event, sb);

        assertEquals(expected, sb.toString());
    }
}
