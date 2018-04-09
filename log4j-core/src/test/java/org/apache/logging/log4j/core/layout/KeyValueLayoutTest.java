package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.lookup.MainMapLookup;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.junit.ThreadContextRule;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class KeyValueLayoutTest {

    static ConfigurationFactory cf = new BasicConfigurationFactory();

    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    @BeforeClass
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
    }

    LoggerContext ctx = LoggerContext.getContext();

    @Rule
    public final ThreadContextRule threadContextRule = new ThreadContextRule();

    private static class Destination implements ByteBufferDestination {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[2048]);
        @Override
        public ByteBuffer getByteBuffer() {
            return byteBuffer;
        }

        @Override
        public ByteBuffer drain(final ByteBuffer buf) {
            throw new IllegalStateException("Unexpected message larger than 2048 bytes");
        }

        @Override
        public void writeBytes(final ByteBuffer data) {
            byteBuffer.put(data);
        }

        @Override
        public void writeBytes(final byte[] data, final int offset, final int length) {
            byteBuffer.put(data, offset, length);
        }
    }

    private void assertToByteArray(final String expectedStr, final KeyValueLayout layout, final LogEvent event) {
        final byte[] result = layout.toByteArray(event);
        assertEquals(expectedStr, new String(result));
    }

    private void assertEncode(final String expectedStr, final KeyValueLayout layout, final LogEvent event) {
        final Destination destination = new Destination();
        layout.encode(event, destination);
        final ByteBuffer byteBuffer = destination.getByteBuffer();
        byteBuffer.flip(); // set limit to position, position back to zero
        assertEquals(expectedStr, new String(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(),
            byteBuffer.remaining()));
    }



    @Test
    public void testSimpleMessage() throws Exception {
        final KeyValueLayout layout = KeyValueLayout.newBuilder().withKeyValue(new KeyValuePair("logger", "%logger"))
            .withConfiguration(ctx.getConfiguration()).build();
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
            .setLoggerName(this.getClass().getName()).setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
            .setLevel(Level.INFO) //
            .setMarker(MarkerManager.getMarker("TestMarker")) //
            .setMessage(new SimpleMessage("Hello, world!")).build();
        assertToByteArray("logger=\"org.apache.logging.log4j.core.layout.KeyValueLayoutTest\" message=\"Hello, world!\"\n", layout,
            event1
        );
        assertEncode("logger=\"org.apache.logging.log4j.core.layout.KeyValueLayoutTest\" message=\"Hello, world!\"\n", layout,
            event1
        );
    }

    @Test
    public void testParameterizedMessage() throws Exception {
        final KeyValueLayout layout = KeyValueLayout.newBuilder().withKeyValue(new KeyValuePair("logger", "%logger"))
            .withConfiguration(ctx.getConfiguration()).build();
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
            .setLoggerName(this.getClass().getName()).setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
            .setLevel(Level.INFO) //
            .setMarker(MarkerManager.getMarker("TestMarker")) //
            .setMessage(new ParameterizedMessage("Hello, {}!", "world")).build();
        assertToByteArray("logger=\"org.apache.logging.log4j.core.layout.KeyValueLayoutTest\" message=\"Hello, world!\"\n", layout,
            event1
        );
        assertEncode("logger=\"org.apache.logging.log4j.core.layout.KeyValueLayoutTest\" message=\"Hello, world!\"\n", layout,
            event1
        );
    }


    @Test
    public void testMapMessage() throws Exception {
        final KeyValueLayout layout = KeyValueLayout.newBuilder().withKeyValue(new KeyValuePair("logger", "%logger"))
            .withConfiguration(ctx.getConfiguration()).build();

        StringMapMessage mapMessage = new StringMapMessage();
        mapMessage.put("message", "Hello, world!");
        mapMessage.put("key1", "this \" is map\"");

        final LogEvent event1 = Log4jLogEvent.newBuilder() //
            .setLoggerName(this.getClass().getName()).setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
            .setLevel(Level.INFO) //
            .setMarker(MarkerManager.getMarker("TestMarker")) //
            .setMessage(mapMessage).build();
        assertToByteArray("logger=\"org.apache.logging.log4j.core.layout.KeyValueLayoutTest\" key1=\"this \\\" is map\\\"\" message=\"Hello, world!\"\n", layout,
            event1);
        assertEncode("logger=\"org.apache.logging.log4j.core.layout.KeyValueLayoutTest\" key1=\"this \\\" is map\\\"\" message=\"Hello, world!\"\n", layout,
            event1);
    }


    @Test
    public void testLayoutWithNonDefaultEndOfLine() throws Exception {
        final KeyValueLayout layout = KeyValueLayout.newBuilder().withKeyValue(new KeyValuePair("logger", "%logger"))
            .withConfiguration(ctx.getConfiguration()).build();

        StringMapMessage mapMessage = new StringMapMessage();
        mapMessage.put("message", "Hello, world!");
        mapMessage.put("key1", "this \" is map\"");
        mapMessage.put("quotes", "\"should have \"\"\"\"quot es\"");

        final LogEvent event1 = Log4jLogEvent.newBuilder() //
            .setLoggerName(this.getClass().getName()).setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
            .setLevel(Level.INFO) //
            .setMarker(MarkerManager.getMarker("TestMarker")) //
            .setMessage(mapMessage).build();
        assertToByteArray("logger=\"org.apache.logging.log4j.core.layout.KeyValueLayoutTest\" key1=\"this \\\" is map\\\"\" message=\"Hello, world!\" quotes=\"\\\"should have \\\"\\\"\\\"\\\"quot es\\\"\"\n", layout,
            event1);
        assertEncode("logger=\"org.apache.logging.log4j.core.layout.KeyValueLayoutTest\" key1=\"this \\\" is map\\\"\" message=\"Hello, world!\" quotes=\"\\\"should have \\\"\\\"\\\"\\\"quot es\\\"\"\n", layout,
            event1);
    }

    @Test
    public void testLayoutConfiguredFromXml() throws Exception {

        Configuration configuration = new XmlConfiguration(ctx, ConfigurationSource.fromResource("log4j-keyvaluelayout.xml", getClass().getClassLoader()));
        configuration.initialize();
        Layout<? extends Serializable> layout = configuration.getAppender("STDOUT").getLayout();
        assertTrue(layout instanceof KeyValueLayout);

        final LogEvent event1 = Log4jLogEvent.newBuilder() //
            .setLoggerName(this.getClass().getName()).setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
            .setLevel(Level.INFO) //
            .setMarker(MarkerManager.getMarker("TestMarker")) //
            .setMessage(new ParameterizedMessage("Hello, {}!", "world")).build();
        assertToByteArray("string=Hello, world!org.apache.logging.log4j.core.Logger", (KeyValueLayout) layout,
            event1
        );
        assertEncode("string=Hello, world!org.apache.logging.log4j.core.Logger", (KeyValueLayout) layout,
            event1
        );
    }
}