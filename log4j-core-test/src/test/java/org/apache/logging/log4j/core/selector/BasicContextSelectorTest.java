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
package org.apache.logging.log4j.core.selector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.util.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public final class BasicContextSelectorTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, BasicContextSelector.class.getName());
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(Constants.LOG4J_CONTEXT_SELECTOR);
    }

    @Test
    public void testLogManagerShutdown() {
        final LoggerContext context = (LoggerContext) LogManager.getContext();
        assertEquals(LifeCycle.State.STARTED, context.getState());
        LogManager.shutdown();
        assertEquals(LifeCycle.State.STOPPED, context.getState());
    }

    @Test
    public void testNotDependentOnClassLoader() {
        assertFalse(LogManager.getFactory().isClassLoaderDependent());
    }
}
