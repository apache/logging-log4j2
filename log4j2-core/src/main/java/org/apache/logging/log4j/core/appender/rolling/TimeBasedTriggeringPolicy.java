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
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 *
 */

@Plugin(name = "TimeBasedTriggeringPolicy", type = "Core", printObject = true)
public class TimeBasedTriggeringPolicy implements TriggeringPolicy {

    private long nextRollover;

    private RollingFileManager manager;

    public void initialize(RollingFileManager manager) {
        this.manager = manager;
        nextRollover = manager.getProcessor().getNextTime(manager.getFileTime());
    }

    public boolean isTriggeringEvent(LogEvent event) {
        long now = System.currentTimeMillis();
        if (now > nextRollover) {
            nextRollover = manager.getProcessor().getNextTime(now);
            return true;
        }
        return false;
    }

    @PluginFactory
    public static TimeBasedTriggeringPolicy createPolicy() {
        return new TimeBasedTriggeringPolicy();
    }
}
