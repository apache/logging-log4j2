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
package org.apache.logging.log4j.core.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.junit.jupiter.api.Test;

public class CompositeConfigurationPostConfigureTest {

    private static final String MARKER = "programmaticMarker";

    private static final byte[] XML = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<Configuration><Loggers><Root level=\"INFO\"/></Loggers></Configuration>\n")
            .getBytes(StandardCharsets.UTF_8);

    /** A configuration that contributes a programmatic appender through the {@code postConfigure} hook. */
    private static final class ContributingConfiguration extends XmlConfiguration {
        ContributingConfiguration(final LoggerContext ctx, final ConfigurationSource source) {
            super(ctx, source);
        }

        @Override
        public void postConfigure(final Configuration target) {
            if (target.getAppender(MARKER) == null) {
                target.addAppender(NullAppender.createAppender(MARKER));
            }
        }
    }

    private static ConfigurationSource streamSource() throws Exception {
        return new ConfigurationSource(new ByteArrayInputStream(XML));
    }

    private static CompositeConfiguration composite(final LoggerContext ctx) throws Exception {
        final AbstractConfiguration contributing = new ContributingConfiguration(ctx, streamSource());
        final AbstractConfiguration plain = new XmlConfiguration(ctx, streamSource());
        return new CompositeConfiguration(Arrays.asList(contributing, plain));
    }

    @Test
    public void postConfigureRunsOnInitialBuild() throws Exception {
        final LoggerContext ctx = new LoggerContext("postConfigureInitial");
        final CompositeConfiguration composite = composite(ctx);
        composite.initialize();
        assertNotNull(composite.getAppender(MARKER), "postConfigure contribution missing from the composite");
    }

    @Test
    public void postConfigureRunsAgainOnReconfigure() throws Exception {
        final LoggerContext ctx = new LoggerContext("postConfigureReconfigure");
        final CompositeConfiguration composite = composite(ctx);
        composite.initialize();
        final Appender first = composite.getAppender(MARKER);
        assertNotNull(first, "postConfigure contribution missing after the initial build");

        final Configuration reconfigured = composite.reconfigure();
        assertNotNull(reconfigured, "reconfigure() returned null");
        reconfigured.initialize();
        final Appender second = reconfigured.getAppender(MARKER);
        assertNotNull(second, "postConfigure contribution dropped on reconfigure");
        assertNotSame(first, second, "reconfigure should rebuild the contribution on the new configuration");
    }
}
