package org.apache.logging.log4j.core.impl;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

public class Log4jLogEventTest {

    @Test
    public void testJavaIoSerializable() throws Exception {
        Log4jLogEvent evt = new Log4jLogEvent("some.test", null, "",
                Level.INFO, new SimpleMessage("abc"), null);
        
        ByteArrayOutputStream arr = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(arr);
        out.writeObject(evt);
        
        ByteArrayInputStream inArr = new ByteArrayInputStream(arr.toByteArray());
        ObjectInputStream in = new ObjectInputStream(inArr);
        Log4jLogEvent evt2 = (Log4jLogEvent) in.readObject();
        
        assertEquals(evt.getMillis(), evt2.getMillis());
        assertEquals(evt.getFQCN(), evt2.getFQCN());
        assertEquals(evt.getLevel(), evt2.getLevel());
        assertEquals(evt.getLoggerName(), evt2.getLoggerName());
        assertEquals(evt.getMarker(), evt2.getMarker());
        assertEquals(evt.getContextMap(), evt2.getContextMap());
        assertEquals(evt.getContextStack(), evt2.getContextStack());
        assertEquals(evt.getMessage(), evt2.getMessage());
        assertEquals(evt.getSource(), evt2.getSource());
        assertEquals(evt.getThreadName(), evt2.getThreadName());
        assertEquals(evt.getThrown(), evt2.getThrown());
        assertEquals(evt.isEndOfBatch(), evt2.isEndOfBatch());
        assertEquals(evt.isIncludeLocation(), evt2.isIncludeLocation());
    }

}
