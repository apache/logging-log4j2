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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ConfigurationStoppingAwareTest
{

    @Test
    public void testBeforeStop() throws Exception {

        int numberOfAppenders = 3;

        AbstractConfiguration abstractConfiguration = new AbstractConfigurationTestImpl(numberOfAppenders);
        abstractConfiguration.initialize();
        abstractConfiguration.stop();

        List<AppenderTester> appenders = new ArrayList<>();
        for (Appender appender : abstractConfiguration.getAppenders().values()) {
            if (appender instanceof AppenderTester) {
                appenders.add((AppenderTester) appender);
            }
        }

        assertEquals("wrong number of ConfigurationStoppingAware appenders", numberOfAppenders, appenders.size());

        for (Appender appender : appenders) {
            assertEquals("appender beforeStopConfiguration() was called the wrong number of times",
                         1,
                         ((AppenderTester) appender).getBeforeStopConfigurationCount());
        }

    }

    private static class AbstractConfigurationTestImpl extends AbstractConfiguration {

        public AbstractConfigurationTestImpl(int numberOfAppenders) {
            super(null, ConfigurationSource.NULL_SOURCE);


            PluginType<AppendersPlugin> appendersPlugin = mock(PluginType.class);
            when(appendersPlugin.getPluginClass()).thenReturn(AppendersPlugin.class);
            Node appendersNode = new Node(rootNode, "Appenders", appendersPlugin);

            for (int i = 0; i < numberOfAppenders; i++)
            {
                addAppenderTester(appendersNode);
            }

            rootNode = new Node();
            rootNode.getChildren().add(appendersNode);
        }

        private void addAppenderTester(Node appendersNode) {
            PluginType<AppenderTester> appenderPlugin = mock(PluginType.class);
            when(appenderPlugin.getPluginClass()).thenReturn(AppenderTester.class);
            Node appenderNode = new Node(appendersNode, "Appender", appenderPlugin);
            appendersNode.getChildren().add(appenderNode);
        }

    }

    private static class AppenderTester implements Appender, ConfigurationStopAware {

        private final String name;
        private boolean started = false;
        private int beforeStopConfigurationCount = 0;

        public AppenderTester(String name) {
            this.name = name;
        }

        @Override
        public void append(LogEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Layout<? extends Serializable> getLayout() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean ignoreExceptions() {
            return false;
        }

        @Override
        public ErrorHandler getHandler() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setHandler(ErrorHandler handler) {}

        @Override
        public State getState() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void initialize() {}

        @Override
        public void start() {
            started = true;
        }

        @Override
        public void stop() {
            started = false;
        }

        @Override
        public boolean isStarted() {
            return started;
        }

        @Override
        public boolean isStopped() {
            return !started;
        }

        @Override
        public void beforeStopConfiguration() {
            beforeStopConfigurationCount++;
        }

        public int getBeforeStopConfigurationCount() {
            return beforeStopConfigurationCount;
        }

        @PluginBuilderFactory
        public static <B extends AppenderTester.Builder<B>> B newBuilder() {
            return new AppenderTester.Builder<B>().asBuilder();
        }

        public static class Builder<B extends Builder<B>>
                implements org.apache.logging.log4j.core.util.Builder<AppenderTester> {
            
            public B asBuilder() {
                return (B) this;
            }

            @Override
            public AppenderTester build() {
                return new AppenderTester(UUID.randomUUID().toString());
            }
        }
    }
}
