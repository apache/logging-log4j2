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

import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.junit.ThreadContextMapRule;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author jerry on 16/8/16.
 */
public class MdcThrowablePatternConverterTest {

    @Rule
    public final ThreadContextMapRule threadContextRule = new ThreadContextMapRule();

    @Before
    public void setup() {
        ThreadContext.put("subject", "I");
        ThreadContext.put("verb", "love");
        ThreadContext.put("object", "Log4j");
    }

    @Test
    public void testConvertWithNoKey() {
        final Message msg = new SimpleMessage("Hello");
        final Throwable throwable = new Exception("test exception");
        final MdcThrowablePatternConverter converter = MdcThrowablePatternConverter.newInstance(null);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .setThrown(throwable)//
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();

        final String suffix = "{object=Log4j, subject=I, verb=love}";
        String[] lines = str.split(Pattern.quote(Constants.LINE_SEPARATOR));
        for (String line : lines) {
            assertTrue("expect line \"" + line + "\" ends with \"" + suffix + "\", but was not", line.endsWith(suffix));
        }
    }

    @Test
    public void testConvertWithOneKey() {
        final Message msg = new SimpleMessage("Hello");
        final Throwable throwable = new Exception("test exception");
        final MdcThrowablePatternConverter converter = MdcThrowablePatternConverter.newInstance(new String[]{"verb"});
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .setThrown(throwable)//
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();

        final String suffix = " love";
        String[] lines = str.split(Pattern.quote(Constants.LINE_SEPARATOR));
        for (String line : lines) {
            assertTrue("expect line \"" + line + "\" ends with \"" + suffix + "\", but was not", line.endsWith(suffix));
        }
    }

    @Test
    public void testConvertWithMultiKey() {
        final Message msg = new SimpleMessage("Hello");
        final Throwable throwable = new Exception("test exception");
        final MdcThrowablePatternConverter converter = MdcThrowablePatternConverter.newInstance(new String[]{"verb, object"});
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("MyLogger") //
                .setLevel(Level.DEBUG) //
                .setMessage(msg) //
                .setThrown(throwable)//
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String str = sb.toString();

        final String suffix = "{object=Log4j, verb=love}";
        String[] lines = str.split(Pattern.quote(Constants.LINE_SEPARATOR));
        for (String line : lines) {
            assertTrue("expect line \"" + line + "\" ends with \"" + suffix + "\", but was not", line.endsWith(suffix));
        }
    }

}
