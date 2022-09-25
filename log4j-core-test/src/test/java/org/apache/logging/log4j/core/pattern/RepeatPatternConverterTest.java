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
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests that process ID succeeds.
 */
public class RepeatPatternConverterTest {
    @Test
    public void repeat() {
        final String[] args = {"*", "10"};
        final String expected = "**********";
        PatternConverter converter = RepeatPatternConverter.newInstance(null, args);
        assertNotNull(converter, "No RepeatPatternConverter returned");
        StringBuilder sb = new StringBuilder();
        converter.format(null, sb);
        assertEquals(expected, sb.toString());
        sb.setLength(0);
        LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("Hello")).build();
        converter.format(event, sb);
        assertEquals(expected, sb.toString());
    }


}