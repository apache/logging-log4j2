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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.IOException;

import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class LogManagerTest {

    class Inner {
        final Logger LOGGER = LogManager.getLogger();
    }
    
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
        assertNotNull("No Logger returned", logger);
        assertTrue("Incorrect Logger name: " + logger.getName(),LogManagerTest.class.getName().equals(logger.getName()));
        logger = LogManager.getLogger(ParameterizedMessageFactory.INSTANCE);
        assertNotNull("No Logger returned", logger);
        assertTrue("Incorrect Logger name: " + logger.getName(),LogManagerTest.class.getName().equals(logger.getName()));
        logger = LogManager.getLogger((Class<?>) null);
        assertNotNull("No Logger returned", logger);
        assertTrue("Incorrect Logger name: " + logger.getName(),LogManagerTest.class.getName().equals(logger.getName()));
        logger = LogManager.getLogger((Class<?>) null, ParameterizedMessageFactory.INSTANCE);
        assertNotNull("No Logger returned", logger);
        assertTrue("Incorrect Logger name: " + logger.getName(),LogManagerTest.class.getName().equals(logger.getName()));
        logger = LogManager.getLogger((String) null);
        assertNotNull("No Logger returned", logger);
        assertTrue("Incorrect Logger name: " + logger.getName(),LogManagerTest.class.getName().equals(logger.getName()));
        logger = LogManager.getLogger((String) null, ParameterizedMessageFactory.INSTANCE);
        assertNotNull("No Logger returned", logger);
        assertTrue("Incorrect Logger name: " + logger.getName(),LogManagerTest.class.getName().equals(logger.getName()));
        logger = LogManager.getLogger((Object) null);
        assertNotNull("No Logger returned", logger);
        assertTrue("Incorrect Logger name: " + logger.getName(),LogManagerTest.class.getName().equals(logger.getName()));
        logger = LogManager.getLogger((Object) null, ParameterizedMessageFactory.INSTANCE);
        assertNotNull("No Logger returned", logger);
        assertTrue("Incorrect Logger name: " + logger.getName(),LogManagerTest.class.getName().equals(logger.getName()));
    }

    @Test
    public void testGetLoggerForAnonymousInnerClass1() throws IOException {
        final Closeable closeable = new Closeable() {
            
            Logger LOGGER = LogManager.getLogger();
            
            @Override
            public void close() throws IOException {
                Assert.assertEquals("org.apache.logging.log4j.LogManagerTest$1", LOGGER.getName());
            }
        };
        closeable.close();
    }

    @Test
    public void testGetLoggerForAnonymousInnerClass2() throws IOException {
        final Closeable closeable = new Closeable() {
            
            Logger LOGGER = LogManager.getLogger(getClass());
            
            @Override
            public void close() throws IOException {
                Assert.assertEquals("org.apache.logging.log4j.LogManagerTest$2", LOGGER.getName());
            }
        };
        closeable.close();
    }

    @Test
    public void testGetLoggerForInner() {
        Assert.assertEquals("org.apache.logging.log4j.LogManagerTest.Inner", new Inner().LOGGER.getName());
    }

    @Test
    public void testGetLoggerForInnerByClass() {
        Assert.assertEquals("org.apache.logging.log4j.LogManagerTest.InnerByClass", new InnerByClass().LOGGER.getName());
    }

    @Test
    public void testGetLoggerForStaticInner() {
        Assert.assertEquals("org.apache.logging.log4j.LogManagerTest.StaticInner", StaticInner.LOGGER.getName());
    }

    @Test
    public void testGetLoggerForStaticInnerByClass() {
        Assert.assertEquals("org.apache.logging.log4j.LogManagerTest.StaticInnerByClass", StaticInnerByClass.LOGGER.getName());
    }

    @Test
    public void testShutdown() {
        final LoggerContext loggerContext = LogManager.getContext(false);
    }
}
