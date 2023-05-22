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
package org.apache.logging.log4j.core;

import java.io.File;
import java.util.stream.Stream;

import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.async.BasicAsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.BasicContextSelector;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

@Tag("functional")
public class LateConfigTest {

    private static final String CONFIG = "target/test-classes/log4j-test1.xml";
    // This class will be the caller of `Log4jContextFactory`
    private static final String FQCN = Log4jContextFactory.class.getName();

    static Stream<Log4jContextFactory> selectors() {
        final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();
        return Stream
                .<ContextSelector>of(
                        new ClassLoaderContextSelector(instanceFactory.newChildInstanceFactory()),
                        new BasicContextSelector(instanceFactory.newChildInstanceFactory()),
                        new AsyncLoggerContextSelector(instanceFactory.newChildInstanceFactory()),
                        new BasicAsyncLoggerContextSelector(instanceFactory.newChildInstanceFactory()))
                .map(Log4jContextFactory::new);
    }

    @ParameterizedTest(name = "reconfigure {0}")
    @MethodSource("selectors")
    public void testReconfiguration(final Log4jContextFactory factory) throws Exception {
        LoggerContext context = factory.getContext(FQCN, null, null, false);
        final Configuration cfg = context.getConfiguration();
        assertNotNull(cfg, "No configuration");
        assertTrue(cfg instanceof DefaultConfiguration, "Not set to default configuration");
        final File file = new File(CONFIG);
        final LoggerContext loggerContext = factory.getContext(FQCN, null, null, false, file.toURI(), null);
        assertNotNull(loggerContext, "No Logger Context");
        final Configuration newConfig = loggerContext.getConfiguration();
        assertNotSame(cfg, newConfig, "Configuration not reset");
        assertTrue(newConfig instanceof XmlConfiguration, "Reconfiguration failed");
        context = factory.getContext(FQCN, null, null, false);
        final Configuration sameConfig = context.getConfiguration();
        assertSame(newConfig, sameConfig, "Configuration should not have been reset");
    }
}
