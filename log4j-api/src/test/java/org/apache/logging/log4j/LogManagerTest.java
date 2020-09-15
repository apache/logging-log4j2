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
package org.apache.logging.log4j;

import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.Closeable;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ResourceLock(value = "log4j2.LoggerContextFactory", mode = ResourceAccessMode.READ)
public class LogManagerTest {

    @SuppressWarnings("InnerClassMayBeStatic")
    class Inner {
        final Logger LOGGER = LogManager.getLogger();
    }
    
    @SuppressWarnings("InnerClassMayBeStatic")
    class InnerByClass {
        final Logger LOGGER = LogManager.getLogger(InnerByClass.class);
    }
    
    static class StaticInner {
        final static Logger LOGGER = LogManager.getLogger();
    }
    
    static class StaticInnerByClass {
        final static Logger LOGGER = LogManager.getLogger(StaticInnerByClass.class);
    }
    
    @Test
    public void testGetLogger() {
        Logger logger = LogManager.getLogger();
        assertNotNull(logger, "No Logger returned");
        assertEquals(LogManagerTest.class.getName(), logger.getName(), "Incorrect Logger name: " + logger.getName());
        logger = LogManager.getLogger(ParameterizedMessageFactory.INSTANCE);
        assertNotNull(logger, "No Logger returned");
        assertEquals(LogManagerTest.class.getName(), logger.getName(), "Incorrect Logger name: " + logger.getName());
        logger = LogManager.getLogger((Class<?>) null);
        assertNotNull(logger, "No Logger returned");
        assertEquals(LogManagerTest.class.getName(), logger.getName(), "Incorrect Logger name: " + logger.getName());
        logger = LogManager.getLogger((Class<?>) null, ParameterizedMessageFactory.INSTANCE);
        assertNotNull(logger, "No Logger returned");
        assertEquals(LogManagerTest.class.getName(), logger.getName(), "Incorrect Logger name: " + logger.getName());
        logger = LogManager.getLogger((String) null);
        assertNotNull(logger, "No Logger returned");
        assertEquals(LogManagerTest.class.getName(), logger.getName(), "Incorrect Logger name: " + logger.getName());
        logger = LogManager.getLogger((String) null, ParameterizedMessageFactory.INSTANCE);
        assertNotNull(logger, "No Logger returned");
        assertEquals(LogManagerTest.class.getName(), logger.getName(), "Incorrect Logger name: " + logger.getName());
        logger = LogManager.getLogger((Object) null);
        assertNotNull(logger, "No Logger returned");
        assertEquals(LogManagerTest.class.getName(), logger.getName(), "Incorrect Logger name: " + logger.getName());
        logger = LogManager.getLogger((Object) null, ParameterizedMessageFactory.INSTANCE);
        assertNotNull(logger, "No Logger returned");
        assertEquals(LogManagerTest.class.getName(), logger.getName(), "Incorrect Logger name: " + logger.getName());
    }

    @Test
    public void testGetLoggerForAnonymousInnerClass1() throws IOException {
        final Closeable closeable = new Closeable() {
            
            final Logger LOGGER = LogManager.getLogger();
            
            @Override
            public void close() throws IOException {
                assertEquals("org.apache.logging.log4j.LogManagerTest$1", LOGGER.getName());
            }
        };
        closeable.close();
    }

    @Test
    public void testGetLoggerForAnonymousInnerClass2() throws IOException {
        final Closeable closeable = new Closeable() {
            
            final Logger LOGGER = LogManager.getLogger(getClass());
            
            @Override
            public void close() throws IOException {
                assertEquals("org.apache.logging.log4j.LogManagerTest$2", LOGGER.getName());
            }
        };
        closeable.close();
    }

    @Test
    public void testGetLoggerForInner() {
        assertEquals("org.apache.logging.log4j.LogManagerTest.Inner", new Inner().LOGGER.getName());
    }

    @Test
    public void testGetLoggerForInnerByClass() {
        assertEquals("org.apache.logging.log4j.LogManagerTest.InnerByClass", new InnerByClass().LOGGER.getName());
    }

    @Test
    public void testGetLoggerForStaticInner() {
        assertEquals("org.apache.logging.log4j.LogManagerTest.StaticInner", StaticInner.LOGGER.getName());
    }

    @Test
    public void testGetLoggerForStaticInnerByClass() {
        assertEquals("org.apache.logging.log4j.LogManagerTest.StaticInnerByClass", StaticInnerByClass.LOGGER.getName());
    }

    @Test
    public void testShutdown() {
        final LoggerContext loggerContext = LogManager.getContext(false);
    }
}
