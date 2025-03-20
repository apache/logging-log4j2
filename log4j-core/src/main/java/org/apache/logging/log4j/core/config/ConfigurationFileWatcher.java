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
package org.apache.logging.log4j.core.config;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.util.AbstractWatcher;
import org.apache.logging.log4j.core.util.FileWatcher;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.core.util.Watcher;

/**
 * Watcher for configuration files. Causes a reconfiguration when a file changes.
 */
public class ConfigurationFileWatcher extends AbstractWatcher implements FileWatcher {

    private Map<File, Long> monitoredFiles = new HashMap<>();

    public ConfigurationFileWatcher(
            final Configuration configuration,
            final Reconfigurable reconfigurable,
            final List<ConfigurationListener> configurationListeners,
            long lastModifiedMillis) {
        super(configuration, reconfigurable, configurationListeners);
    }

    @Override
    public long getLastModified() {
        Long lastModifiedMillis = 0L;
        for (final File monitoredFile : monitoredFiles.keySet()) {
            if (monitoredFile.lastModified() > lastModifiedMillis) {
                lastModifiedMillis = monitoredFile.lastModified();
            }
        }
        return lastModifiedMillis;
    }

    @Override
    public void fileModified(final File file) {
        monitoredFiles.entrySet().stream()
                .forEach(monitoredFile ->
                        monitoredFile.setValue(monitoredFile.getKey().lastModified()));
    }

    @Override
    public void watching(final Source source) {
        File file = source.getFile();
        monitoredFiles.put(file, file.lastModified());
        super.watching(source);
    }

    /**
     * Add the given URIs to be watched.
     *
     * @param monitorUris URIs to also watch
     */
    public void addMonitorUris(final List<URI> monitorUris) {
        monitorUris.forEach(uri -> {
            File additionalFile = new Source(uri).getFile();
            monitoredFiles.put(additionalFile, additionalFile.lastModified());
        });
    }

    @Override
    public boolean isModified() {
        return monitoredFiles.entrySet().stream()
                .anyMatch(file -> file.getValue() != file.getKey().lastModified());
    }

    @Override
    public Watcher newWatcher(
            final Reconfigurable reconfigurable, final List<ConfigurationListener> listeners, long lastModifiedMillis) {
        final ConfigurationFileWatcher watcher =
                new ConfigurationFileWatcher(getConfiguration(), reconfigurable, listeners, lastModifiedMillis);
        if (getSource() != null) {
            watcher.watching(getSource());
        }
        return watcher;
    }
}
