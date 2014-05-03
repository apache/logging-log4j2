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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class SerializedLayoutTest {
    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("");

    static ConfigurationFactory cf = new BasicConfigurationFactory();

    @BeforeClass
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
    }

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    private static final String body =
        "<log4j:message><![CDATA[empty mdc]]></log4j:message>\r";

    private static final String[] expected = {
        "Logger=root Level=DEBUG Message=starting mdc pattern test",
        "Logger=root Level=DEBUG Message=empty mdc",
        "Logger=root Level=DEBUG Message=filled mdc",
        "Logger=root Level=ERROR Message=finished mdc pattern test",
        "Logger=root Level=ERROR Message=Throwing an exception"
    };


    /**
     * Test case for MDC conversion pattern.
     */
    @Test
    public void testLayout() throws Exception {
        final Map<String, Appender> appenders = root.getAppenders();
        for (Appender appender : appenders.values()) {
            root.removeAppender(appender);
        }
        // set up appender
        final SerializedLayout layout = SerializedLayout.createLayout();
        final ListAppender appender = new ListAppender("List", null, layout, false, true);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        // output starting message
        root.debug("starting mdc pattern test");

        root.debug("empty mdc");

        ThreadContext.put("key1", "value1");
        ThreadContext.put("key2", "value2");

        root.debug("filled mdc");

        ThreadContext.remove("key1");
        ThreadContext.remove("key2");

        root.error("finished mdc pattern test", new NullPointerException("test"));

        final Exception parent = new IllegalStateException("Test");
        final Throwable child = new LoggingException("This is a test", parent);

        root.error("Throwing an exception", child);

        appender.stop();

        final List<byte[]> data = appender.getData();
        assertTrue(data.size() > 0);
        int i = 0;
        for (final byte[] item : data) {
            final ByteArrayInputStream bais = new ByteArrayInputStream(item);
            final ObjectInputStream ois = new ObjectInputStream(bais);
            LogEvent event;
            try {
                event = (LogEvent) ois.readObject();
            } catch (final IOException ioe) {
                System.err.println("Exception processing item " + i);
                throw ioe;
            }
            assertTrue("Incorrect event", event.toString().equals(expected[i]));
            ++i;
        }
        for (Appender app : appenders.values()) {
            root.addAppender(app);
        }
    }

    @Test
    public void testSerialization() throws Exception {
        final SerializedLayout layout = SerializedLayout.createLayout();
        final Throwable throwable = new LoggingException("Test");
        final LogEvent event = new Log4jLogEvent(this.getClass().getName(), null,
            "org.apache.logging.log4j.core.Logger", Level.INFO, new SimpleMessage("Hello, world!"), throwable);
        final byte[] result = layout.toByteArray(event);
        assertNotNull(result);
        //FileOutputStream fos = new FileOutputStream("target/serializedEvent.dat");
        //fos.write(layout.getHeader());
        //fos.write(result);
        //fos.close();
    }

    @Test
    public void testDeserialization() throws Exception {
        File file = new File("target/test-classes/serializedEvent.dat");
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);
        final LogEvent event = (LogEvent) ois.readObject();
        assertNotNull(event);
    }
}
