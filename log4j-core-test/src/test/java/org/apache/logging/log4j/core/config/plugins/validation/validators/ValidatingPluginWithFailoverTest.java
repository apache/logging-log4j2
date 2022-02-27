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
package org.apache.logging.log4j.core.config.plugins.validation.validators;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.appender.FailoverAppender;
import org.apache.logging.log4j.core.appender.FailoversPlugin;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.plugins.util.PluginManager;
import org.apache.logging.log4j.plugins.util.PluginType;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.StatusLoggerLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@StatusLoggerLevel("OFF")
public class ValidatingPluginWithFailoverTest {

    private Node node;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws Exception {
        final PluginManager manager = new PluginManager(Core.CATEGORY_NAME);
        manager.collectPlugins();
        PluginType<FailoverAppender> plugin = (PluginType<FailoverAppender>) manager.getPluginType("failover");
        assertNotNull(plugin, "Rebuild this module to make sure annotation processing kicks in.");

        AppenderRef appenderRef = AppenderRef.createAppenderRef("List", Level.ALL, null);
        node = new Node(null, "failover", plugin);
        Node failoversNode = new Node(node, "Failovers", manager.getPluginType("Failovers"));
        Node appenderRefNode  = new Node(failoversNode, "appenderRef", manager.getPluginType("appenderRef"));
        appenderRefNode.getAttributes().put("ref", "file");
        appenderRefNode.setObject(appenderRef);
        failoversNode.getChildren().add(appenderRefNode);
        failoversNode.setObject(FailoversPlugin.createFailovers(appenderRef));
        node.getAttributes().put("primary", "CONSOLE");
        node.getAttributes().put("name", "Failover");
        node.getChildren().add(failoversNode);
    }

    @Test
    public void testDoesNotLog_NoParameterThatMatchesElement_message() {
        final StatusListener listener = mock(StatusListener.class);
        when(listener.getStatusLevel()).thenReturn(Level.WARN);
        final Injector injector = DI.createInjector()
                .bindInstance(Keys.SUBSTITUTOR_KEY, Function.identity())
                .bindInstance(Key.forClass(Configuration.class), new NullConfiguration());
        final StatusLogger logger = StatusLogger.getLogger();
        logger.trace("Initializing");
        logger.registerListener(listener);
        final FailoverAppender failoverAppender = injector.getInstance(node);

        verify(listener, times(1)).getStatusLevel();
        verify(listener, never()).log(any(StatusData.class));
        verifyNoMoreInteractions(listener);
        assertNotNull(failoverAppender);
        assertEquals("Failover", failoverAppender.getName());
    }
}
