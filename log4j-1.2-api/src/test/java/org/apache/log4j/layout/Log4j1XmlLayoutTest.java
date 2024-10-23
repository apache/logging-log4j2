/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.junit.UsingThreadContextStack;
import org.apache.logging.log4j.util.StringMap;
import org.junit.jupiter.api.Test;

@UsingThreadContextStack
public class Log4j1XmlLayoutTest {

    @Test
    public void testWithoutThrown() {
        final Log4j1XmlLayout layout = Log4j1XmlLayout.createLayout(false, true);

        final Log4jLogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("a.B")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("Hello, World"))
                .setTimeMillis(System.currentTimeMillis() + 17)
                .build();

        final String result = layout.toSerializable(event);

        final String expected = "<log4j:event logger=\"a.B\" timestamp=\"" + event.getTimeMillis()
                + "\" level=\"INFO\" thread=\"main\">\r\n"
                + "<log4j:message><![CDATA[Hello, World]]></log4j:message>\r\n"
                + "</log4j:event>\r\n\r\n";

        assertEquals(expected, result);
    }

    @Test
    public void testWithPropertiesAndLocationInfo() {
        final Log4j1XmlLayout layout = Log4j1XmlLayout.createLayout(true, true);

        final StringMap contextMap = ContextDataFactory.createContextData(2);
        contextMap.putValue("key1", "value1");
        contextMap.putValue("key2", "value2");
        final Log4jLogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("a.B")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("Hello, World"))
                .setTimeMillis(System.currentTimeMillis() + 17)
                .setIncludeLocation(true)
                .setSource(new StackTraceElement("pack.MyClass", "myMethod", "MyClass.java", 17))
                .setContextData(contextMap)
                .build();

        final String result = layout.toSerializable(event);

        final String expected = "<log4j:event logger=\"a.B\" timestamp=\"" + event.getTimeMillis()
                + "\" level=\"INFO\" thread=\"main\">\r\n"
                + "<log4j:message><![CDATA[Hello, World]]></log4j:message>\r\n"
                + "<log4j:locationInfo class=\"pack.MyClass\" method=\"myMethod\" file=\"MyClass.java\" line=\"17\"/>\r\n"
                + "<log4j:properties>\r\n"
                + "<log4j:data name=\"key1\" value=\"value1\"/>\r\n"
                + "<log4j:data name=\"key2\" value=\"value2\"/>\r\n"
                + "</log4j:properties>\r\n"
                + "</log4j:event>\r\n\r\n";

        assertEquals(expected, result);
    }
}
