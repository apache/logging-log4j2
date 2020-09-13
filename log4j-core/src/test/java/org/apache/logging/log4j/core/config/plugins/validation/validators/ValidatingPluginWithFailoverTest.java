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
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ValidatingPluginWithFailoverTest {

    private PluginType<FailoverAppender> plugin;
    private Node node;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws Exception {
        final PluginManager manager = new PluginManager(Core.CATEGORY_NAME);
        manager.collectPlugins();
        plugin = (PluginType<FailoverAppender>) manager.getPluginType("failover");
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
        final StoringStatusListener listener = new StoringStatusListener();
        // @formatter:off
        final PluginBuilder builder = new PluginBuilder(plugin).
                withConfiguration(new NullConfiguration()).
                withConfigurationNode(node);
        // @formatter:on
        StatusLogger.getLogger().registerListener(listener);

        final FailoverAppender failoverAppender = (FailoverAppender) builder.build();

        assertThat(listener.logs, emptyCollectionOf(StatusData.class));
        assertNotNull(failoverAppender);
        assertEquals("Failover", failoverAppender.getName());
    }

    private static class StoringStatusListener implements StatusListener {
        private final List<StatusData> logs = new ArrayList<>();
        @Override
        public void log(StatusData data) {
            logs.add(data);
        }

        @Override
        public Level getStatusLevel() {
            return Level.WARN;
        }

        @Override
        public void close() {}
    }
}
