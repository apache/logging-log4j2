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
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag("async")
@ContextSelectorType(BasicAsyncLoggerContextSelector.class)
public class BasicAsyncLoggerContextSelectorTest {

    private static final String FQCN = BasicAsyncLoggerContextSelectorTest.class.getName();

    @Test
    public void testContextReturnsAsyncLoggerContext() {
        final ContextSelector selector = ((Log4jContextFactory) LogManager.getFactory()).getSelector();
        final LoggerContext context = selector.getContext(FQCN, null, false);

        assertTrue(context instanceof AsyncLoggerContext);
    }

    @Test
    public void testContext2ReturnsAsyncLoggerContext() {
        final ContextSelector selector = ((Log4jContextFactory) LogManager.getFactory()).getSelector();
        final LoggerContext context = selector.getContext(FQCN, null, false, null);

        assertTrue(context instanceof AsyncLoggerContext);
    }

    @Test
    public void testLoggerContextsReturnsAsyncLoggerContext() {
        final ContextSelector selector = ((Log4jContextFactory) LogManager.getFactory()).getSelector();
        assertThat(selector.getLoggerContexts()).hasExactlyElementsOfTypes(AsyncLoggerContext.class);

        selector.getContext(FQCN, null, false);

        assertThat(selector.getLoggerContexts()).hasExactlyElementsOfTypes(AsyncLoggerContext.class);
    }

    @Test
    public void testContextNameIsAsyncDefault() {
        final ContextSelector selector = ((Log4jContextFactory) LogManager.getFactory()).getSelector();
        final LoggerContext context = selector.getContext(FQCN, null, false);
        assertEquals("AsyncDefault", context.getName());
    }

    @Test
    public void testDependentOnClassLoader() {
        final ContextSelector selector = ((Log4jContextFactory) LogManager.getFactory()).getSelector();
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
