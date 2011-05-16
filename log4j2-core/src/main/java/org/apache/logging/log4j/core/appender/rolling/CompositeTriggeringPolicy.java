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
package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.List;

/**
 *
 */
@Plugin(name="Policies",type="Core",printObject=true)
public class CompositeTriggeringPolicy implements TriggeringPolicy {

    private TriggeringPolicy[] policies;

    public CompositeTriggeringPolicy(TriggeringPolicy[] policies) {
        this.policies = policies;
    }

    public void initialize(RollingFileManager manager) {
        for (TriggeringPolicy policy : policies) {
            policy.initialize(manager);
        }
    }

    public boolean isTriggeringEvent(LogEvent event) {
        for (TriggeringPolicy policy : policies) {
            if (policy.isTriggeringEvent(event)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("CompositeTriggeringPolicy{");
        boolean first = true;
        for (TriggeringPolicy policy : policies) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(policy.toString());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @PluginFactory
    public static CompositeTriggeringPolicy createPolicy(@PluginElement("policies") TriggeringPolicy[] policies) {
        return new CompositeTriggeringPolicy(policies);
    }
}
