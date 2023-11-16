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

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 *
 */
@Plugin(name = "SizeBasedTriggeringPolicy", category = Core.CATEGORY_NAME, printObject = true)
public class SizeBasedTriggeringPolicy extends AbstractTriggeringPolicy {

    /**
     * Rollover threshold size in bytes.
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // let 10 MB the default max size

    private final long maxFileSize;

    private RollingFileManager manager;

    /**
     * Constructs a new instance.
     */
    protected SizeBasedTriggeringPolicy() {
        this.maxFileSize = MAX_FILE_SIZE;
    }

    /**
     * Constructs a new instance.
     *
     * @param maxFileSize rollover threshold size in bytes.
     */
    protected SizeBasedTriggeringPolicy(final long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Initialize the TriggeringPolicy.
     * @param aManager The RollingFileManager.
     */
    @Override
    public void initialize(final RollingFileManager aManager) {
        this.manager = aManager;
    }

    /**
     * Returns true if a rollover should occur.
     * @param event   A reference to the currently event.
     * @return true if a rollover should take place, false otherwise.
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent event) {
        final boolean triggered = manager.getFileSize() > maxFileSize;
        if (triggered) {
            manager.getPatternProcessor().updateTime();
        }
        return triggered;
    }

    @Override
    public String toString() {
        return "SizeBasedTriggeringPolicy(size=" + maxFileSize + ')';
    }

    /**
     * Create a SizeBasedTriggeringPolicy.
     * @param size The size of the file before rollover is required.
     * @return A SizeBasedTriggeringPolicy.
     */
    @PluginFactory
    public static SizeBasedTriggeringPolicy createPolicy(@PluginAttribute("size") final String size) {

        final long maxSize = size == null ? MAX_FILE_SIZE : FileSize.parse(size, MAX_FILE_SIZE);
        return new SizeBasedTriggeringPolicy(maxSize);
    }
}
