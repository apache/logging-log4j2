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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.Closeable;
import java.io.IOException;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
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
        assertThat(logger).describedAs("No Logger returned").isNotNull();
        assertThat(logger.getName()).describedAs("Incorrect Logger name: " + logger.getName()).isEqualTo(LogManagerTest.class.getName());
        logger = LogManager.getLogger(ParameterizedMessageFactory.INSTANCE);
        assertThat(logger).describedAs("No Logger returned").isNotNull();
        assertThat(logger.getName()).describedAs("Incorrect Logger name: " + logger.getName()).isEqualTo(LogManagerTest.class.getName());
        logger = LogManager.getLogger((Class<?>) null);
        assertThat(logger).describedAs("No Logger returned").isNotNull();
        assertThat(logger.getName()).describedAs("Incorrect Logger name: " + logger.getName()).isEqualTo(LogManagerTest.class.getName());
        logger = LogManager.getLogger((Class<?>) null, ParameterizedMessageFactory.INSTANCE);
        assertThat(logger).describedAs("No Logger returned").isNotNull();
        assertThat(logger.getName()).describedAs("Incorrect Logger name: " + logger.getName()).isEqualTo(LogManagerTest.class.getName());
        logger = LogManager.getLogger((String) null);
        assertThat(logger).describedAs("No Logger returned").isNotNull();
        assertThat(logger.getName()).describedAs("Incorrect Logger name: " + logger.getName()).isEqualTo(LogManagerTest.class.getName());
        logger = LogManager.getLogger((String) null, ParameterizedMessageFactory.INSTANCE);
        assertThat(logger).describedAs("No Logger returned").isNotNull();
        assertThat(logger.getName()).describedAs("Incorrect Logger name: " + logger.getName()).isEqualTo(LogManagerTest.class.getName());
        logger = LogManager.getLogger((Object) null);
        assertThat(logger).describedAs("No Logger returned").isNotNull();
        assertThat(logger.getName()).describedAs("Incorrect Logger name: " + logger.getName()).isEqualTo(LogManagerTest.class.getName());
        logger = LogManager.getLogger((Object) null, ParameterizedMessageFactory.INSTANCE);
        assertThat(logger).describedAs("No Logger returned").isNotNull();
        assertThat(logger.getName()).describedAs("Incorrect Logger name: " + logger.getName()).isEqualTo(LogManagerTest.class.getName());
    }

    @Test
    public void testGetLoggerForAnonymousInnerClass1() throws IOException {
        final Closeable closeable = new Closeable() {
            
            final Logger LOGGER = LogManager.getLogger();
            
            @Override
            public void close() throws IOException {
                assertThat(LOGGER.getName()).isEqualTo("org.apache.logging.log4j.LogManagerTest$1");
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
                assertThat(LOGGER.getName()).isEqualTo("org.apache.logging.log4j.LogManagerTest$2");
            }
        };
        closeable.close();
    }

    @Test
    public void testGetLoggerForInner() {
        assertThat(new Inner().LOGGER.getName()).isEqualTo("org.apache.logging.log4j.LogManagerTest.Inner");
    }

    @Test
    public void testGetLoggerForInnerByClass() {
        assertThat(new InnerByClass().LOGGER.getName()).isEqualTo("org.apache.logging.log4j.LogManagerTest.InnerByClass");
    }

    @Test
    public void testGetLoggerForStaticInner() {
        assertThat(StaticInner.LOGGER.getName()).isEqualTo("org.apache.logging.log4j.LogManagerTest.StaticInner");
    }

    @Test
    public void testGetLoggerForStaticInnerByClass() {
        assertThat(StaticInnerByClass.LOGGER.getName()).isEqualTo("org.apache.logging.log4j.LogManagerTest.StaticInnerByClass");
    }

    @Test
    public void testShutdown() {
        final LoggerContext loggerContext = LogManager.getContext(false);
    }
}
