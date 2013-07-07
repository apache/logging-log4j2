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
package org.apache.logging.log4j.flume.appender;

import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.flume.node.NodeConfiguration;
import org.apache.flume.node.NodeManager;
import org.apache.flume.node.nodemanager.NodeConfigurationAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 *
 */
public class FlumeNode implements LifecycleAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlumeNode.class);

    private LifecycleState lifecycleState;
    private final NodeManager nodeManager;
    private final NodeConfigurationAware configurationAware;
    private final NodeConfiguration conf;

    public FlumeNode(final NodeConfigurationAware configurationAware, final NodeManager manager,
                     final NodeConfiguration conf) {
        this.nodeManager = manager;
        this.conf = conf;
        this.configurationAware = configurationAware;
    }

    @Override
    public void start() {

        Preconditions.checkState(nodeManager != null, "Node manager can not be null");

        LOGGER.info("Flume node starting");

        configurationAware.startAllComponents(conf);

        lifecycleState = LifecycleState.START;
    }

    @Override
    public void stop() {

        LOGGER.info("Flume node stopping");

        configurationAware.stopAllComponents();

        lifecycleState = LifecycleState.STOP;
    }

    public NodeManager getNodeManager() {
        return nodeManager;
    }

    public NodeConfiguration getConfiguration() {
        return conf;
    }

    @Override
    public LifecycleState getLifecycleState() {
        return lifecycleState;
    }
}
