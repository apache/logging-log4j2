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
package org.apache.logging.log4j.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.core.selector.BasicContextSelector;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.junit.jupiter.api.Test;

public class Log4jContextFactoryTest {

    /**
     * Tests whether the constructor parameters take priority over the default
     * injector bindings.
     */
    @Test
    public void testParameterPriority() {
        final Injector injector = DI.createInjector();
        injector.init();
        Log4jContextFactory factory = new Log4jContextFactory(new BasicContextSelector(injector));
        assertEquals(BasicContextSelector.class, factory.getSelector().getClass());
        factory = new Log4jContextFactory(factory);
        assertEquals(Log4jContextFactory.class, factory.getShutdownCallbackRegistry().getClass());
        factory = new Log4jContextFactory(new BasicContextSelector(injector), factory);
        assertEquals(BasicContextSelector.class, factory.getSelector().getClass());
        assertEquals(Log4jContextFactory.class, factory.getShutdownCallbackRegistry().getClass());
    }
}
