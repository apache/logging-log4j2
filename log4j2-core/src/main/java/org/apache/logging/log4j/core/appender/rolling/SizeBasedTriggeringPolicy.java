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
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 *
 */
@Plugin(name = "Size", type = "Core", printObject = true)
public class SizeBasedTriggeringPolicy implements TriggeringPolicy {

    /**
     * Rollover threshold size in bytes.
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // let 10 MB the default max size

    private final long maxFileSize;

    private RollingFileManager manager;

    /**
     * Constructs a new instance.
     */
    public SizeBasedTriggeringPolicy() {
        this.maxFileSize = MAX_FILE_SIZE;
    }

    public void initialize(RollingFileManager manager) {
        this.manager = manager;
    }

    /**
     * Constructs a new instance.
     *
     * @param maxFileSize rollover threshold size in bytes.
     */
    public SizeBasedTriggeringPolicy(final long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public boolean isTriggeringEvent(LogEvent event) {
        return manager.getFileSize() > maxFileSize;
    }

    @PluginFactory
    public static SizeBasedTriggeringPolicy createPolicy(@PluginAttr("size") String size) {
        long maxSize = size == null ? MAX_FILE_SIZE : Long.parseLong(size);
        return new SizeBasedTriggeringPolicy(maxSize);
    }
}
