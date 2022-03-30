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

import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.plugins.test.validation.HostAndPort;
import org.apache.logging.log4j.plugins.util.PluginManager;
import org.apache.logging.log4j.plugins.util.PluginType;
import org.apache.logging.log4j.test.junit.StatusLoggerLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@StatusLoggerLevel("OFF")
public class ValidHostValidatorTest {

    private final Injector injector = DI.createInjector().registerBinding(Keys.SUBSTITUTOR_KEY, Function::identity);
    private Node node;

    @BeforeEach
    public void setUp() throws Exception {
        final PluginManager pluginManager = injector.getInstance(new @Named("Test") Key<>() {});
        PluginType<?> plugin = pluginManager.getPluginType("HostAndPort");
        assertNotNull(plugin, "Rebuild this module to ensure annotation processing has been done.");
        node = new Node(null, "HostAndPort", plugin);
    }

    @Test
    public void testNullHost() throws Exception {
        assertNull(injector.configure(node));
    }

    @Test
    public void testInvalidIpAddress() throws Exception {
        node.getAttributes().put("host", "256.256.256.256");
        node.getAttributes().put("port", "1");
        final HostAndPort plugin = injector.configure(node);
        assertNull(plugin, "Expected null, but got: " + plugin);
    }

    @Test
    public void testLocalhost() throws Exception {
        node.getAttributes().put("host", "localhost");
        node.getAttributes().put("port", "1");
        final HostAndPort hostAndPort = injector.configure(node);
        assertNotNull(hostAndPort);
        assertTrue(hostAndPort.isValid());
    }

}
