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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.util.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(AsyncLoggers.class)
public class BasicAsyncLoggerContextSelectorTest {

    private static final String FQCN = BasicAsyncLoggerContextSelectorTest.class.getName();

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR,
                BasicAsyncLoggerContextSelector.class.getName());
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(Constants.LOG4J_CONTEXT_SELECTOR);
    }

    @Test
    public void testContextReturnsAsyncLoggerContext() {
        final BasicAsyncLoggerContextSelector selector = new BasicAsyncLoggerContextSelector();
        final LoggerContext context = selector.getContext(FQCN, null, false);

        assertTrue(context instanceof AsyncLoggerContext);
    }

    @Test
    public void testContext2ReturnsAsyncLoggerContext() {
        final BasicAsyncLoggerContextSelector selector = new BasicAsyncLoggerContextSelector();
        final LoggerContext context = selector.getContext(FQCN, null, false, null);

        assertTrue(context instanceof AsyncLoggerContext);
    }

    @Test
    public void testLoggerContextsReturnsAsyncLoggerContext() {
        final BasicAsyncLoggerContextSelector selector = new BasicAsyncLoggerContextSelector();

        List<LoggerContext> list = selector.getLoggerContexts();
        assertEquals(1, list.size());
        assertTrue(list.get(0) instanceof AsyncLoggerContext);

        selector.getContext(FQCN, null, false);

        list = selector.getLoggerContexts();
        assertEquals(1, list.size());
        assertTrue(list.get(0) instanceof AsyncLoggerContext);
    }

    @Test
    public void testContextNameIsAsyncDefault() {
        final BasicAsyncLoggerContextSelector selector = new BasicAsyncLoggerContextSelector();
        final LoggerContext context = selector.getContext(FQCN, null, false);
        assertEquals("AsyncDefault" , context.getName());
    }

    @Test
    public void testDependentOnClassLoader() {
        final BasicAsyncLoggerContextSelector selector = new BasicAsyncLoggerContextSelector();
        assertFalse(selector.isClassLoaderDependent());
    }

    @Test
    public void testFactoryIsNotDependentOnClassLoader() {
        assertFalse(LogManager.getFactory().isClassLoaderDependent());
    }

    @Test
    public void testLogManagerShutdown() {
        LoggerContext context = (LoggerContext) LogManager.getContext();
        assertEquals(LifeCycle.State.STARTED, context.getState());
        LogManager.shutdown();
        assertEquals(LifeCycle.State.STOPPED, context.getState());
    }
}
