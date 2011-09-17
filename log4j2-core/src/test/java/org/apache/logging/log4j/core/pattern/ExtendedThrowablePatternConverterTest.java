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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 */
public class ExtendedThrowablePatternConverterTest {



    @Before
    public void setup() {

    }

    @Test
    public void testFull() {
        ExtendedThrowablePatternConverter converter = ExtendedThrowablePatternConverter.newInstance(null);
        Throwable cause = new NullPointerException("null pointer");
        Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        LogEvent event = new Log4jLogEvent("testLogger", null, this.getClass().getName(), Level.DEBUG,
            new SimpleMessage("test exception"), parent);
        StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        parent.printStackTrace(pw);
        String result = sb.toString();
        //System.out.print(result);
        result = result.replaceAll(" ~?\\[.*\\]", "");
        assertEquals(sw.toString(), result);
    }
}
