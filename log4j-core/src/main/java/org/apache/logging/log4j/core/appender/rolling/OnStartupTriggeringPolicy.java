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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Triggers a rollover on every restart, but only if the file size is greater than zero.
 */
@Plugin(name = "OnStartupTriggeringPolicy", category = "Core", printObject = true)
public class OnStartupTriggeringPolicy implements TriggeringPolicy {
    private RollingFileManager manager;

    /**
     * Provide the RollingFileManager to the policy.
     * @param manager The RollingFileManager.
     */
    @Override
    public void initialize(final RollingFileManager manager) {
        this.manager = manager;
        if (manager.getFileSize() > 0) {
            manager.skipFooter(true);
            manager.rollover();
            manager.skipFooter(false);
        }
    }

    /**
     * Determine if a rollover should be triggered.
     * @param event   A reference to the current event.
     * @return true if the target file's timestamp is older than the JVM start time.
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent event) {
        return false;
    }

    @Override
    public String toString() {
        return "OnStartupTriggeringPolicy";
    }

    @PluginFactory
    public static OnStartupTriggeringPolicy createPolicy() {
        return new OnStartupTriggeringPolicy();
    }
}
