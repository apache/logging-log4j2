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
package org.apache.logging.log4j.core.appender.rolling;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LifeCycle2;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Triggering policy that wraps other triggering policies.
 */
@Plugin(name = "Policies", category = Core.CATEGORY_NAME, printObject = true)
public final class CompositeTriggeringPolicy extends AbstractTriggeringPolicy {

    private final TriggeringPolicy[] triggeringPolicies;

    private CompositeTriggeringPolicy(final TriggeringPolicy... triggeringPolicies) {
        this.triggeringPolicies = triggeringPolicies;
    }

    public TriggeringPolicy[] getTriggeringPolicies() {
        return triggeringPolicies;
    }

    /**
     * Initializes the policy.
     * @param manager The RollingFileManager.
     */
    @Override
    public void initialize(final RollingFileManager manager) {
        for (final TriggeringPolicy triggeringPolicy : triggeringPolicies) {
            LOGGER.debug("Initializing triggering policy {}", triggeringPolicy.toString());
            triggeringPolicy.initialize(manager);
        }
    }

    /**
     * Determines if a rollover should occur.
     * @param event A reference to the currently event.
     * @return true if a rollover should occur, false otherwise.
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent event) {
        for (final TriggeringPolicy triggeringPolicy : triggeringPolicies) {
            if (triggeringPolicy.isTriggeringEvent(event)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a CompositeTriggeringPolicy.
     * @param triggeringPolicy The triggering policies.
     * @return A CompositeTriggeringPolicy.
     */
    @PluginFactory
    public static CompositeTriggeringPolicy createPolicy(
            @PluginElement("Policies") final TriggeringPolicy... triggeringPolicy) {
        return new CompositeTriggeringPolicy(triggeringPolicy);
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        boolean stopped = true;
        for (final TriggeringPolicy triggeringPolicy : triggeringPolicies) {
            if (triggeringPolicy instanceof LifeCycle2) {
                stopped &= ((LifeCycle2) triggeringPolicy).stop(timeout, timeUnit);
            } else if (triggeringPolicy instanceof LifeCycle) {
                ((LifeCycle) triggeringPolicy).stop();
                stopped &= true;
            }
        }
        setStopped();
        return stopped;
    }

    @Override
    public String toString() {
        return "CompositeTriggeringPolicy(policies=" + Arrays.toString(triggeringPolicies) + ")";
    }
}
