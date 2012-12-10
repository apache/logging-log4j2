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

import com.google.common.base.Preconditions;
import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.flume.lifecycle.LifecycleSupervisor;
import org.apache.flume.node.NodeConfiguration;
import org.apache.flume.node.NodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class FlumeNode implements LifecycleAware {

    private static final Logger logger = LoggerFactory.getLogger(FlumeNode.class);

    private LifecycleState lifecycleState;
    private final NodeManager nodeManager;
    private final LifecycleSupervisor supervisor;
    private final NodeConfiguration conf;

    public FlumeNode(final NodeManager manager, final NodeConfiguration conf) {
        this.nodeManager = manager;
        this.conf =conf;
        supervisor = new LifecycleSupervisor();
    }

    public void start() {

        Preconditions.checkState(nodeManager != null,
            "Node manager can not be null");

        supervisor.start();

        logger.info("Flume node starting");

        supervisor.supervise(nodeManager,
            new LifecycleSupervisor.SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);

        lifecycleState = LifecycleState.START;
    }

    public void stop() {

        logger.info("Flume node stopping");

        supervisor.stop();

        lifecycleState = LifecycleState.STOP;
    }

    public NodeManager getNodeManager() {
        return nodeManager;
    }

    public NodeConfiguration getConfiguration() {
        return conf;
    }

    public LifecycleState getLifecycleState() {
        return lifecycleState;
    }

}
