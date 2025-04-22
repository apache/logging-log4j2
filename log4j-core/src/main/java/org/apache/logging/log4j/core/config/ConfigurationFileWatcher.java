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
import java.util.List;
import org.apache.logging.log4j.core.util.AbstractWatcher;
import org.apache.logging.log4j.core.util.FileWatcher;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.core.util.Watcher;

/**
 * Watcher for configuration files. Causes a reconfiguration when a file changes.
 */
public class ConfigurationFileWatcher extends AbstractWatcher implements FileWatcher {

    private File file;
    private long lastModifiedMillis;

    public ConfigurationFileWatcher(
            final Configuration configuration,
            final Reconfigurable reconfigurable,
            final List<ConfigurationListener> configurationListeners,
            long lastModifiedMillis) {
        super(configuration, reconfigurable, configurationListeners);
        this.lastModifiedMillis = lastModifiedMillis;
    }

    @Override
    public long getLastModified() {
        return file != null ? file.lastModified() : 0;
    }

    @Override
    public void fileModified(final File file) {
        lastModifiedMillis = file.lastModified();
    }

    @Override
    public void watching(final Source source) {
        file = source.getFile();
        lastModifiedMillis = file.lastModified();
        super.watching(source);
    }

    @Override
    public boolean isModified() {
        return lastModifiedMillis != file.lastModified();
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
