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
package org.apache.logging.log4j.core.util;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.Reconfigurable;

/**
 *
 */
public class WrappedFileWatcher extends AbstractWatcher implements FileWatcher {

    private final FileWatcher watcher;
    private volatile long lastModifiedMillis;

    public WrappedFileWatcher(FileWatcher watcher, final Configuration configuration,
        final Reconfigurable reconfigurable, final List<ConfigurationListener> configurationListeners,
        final long lastModifiedMillis) {
        super(configuration, reconfigurable, configurationListeners);
        this.watcher = watcher;
        this.lastModifiedMillis = lastModifiedMillis;
    }


    public WrappedFileWatcher(FileWatcher watcher) {
        super(null, null, null);
        this.watcher = watcher;
    }

    @Override
    public long getLastModified() {
        return lastModifiedMillis;
    }

    @Override
    public void fileModified(File file) {
        watcher.fileModified(file);
    }

    @Override
    public boolean isModified() {
        long lastModified = getSource().getFile().lastModified();
        if (lastModifiedMillis != lastModified) {
            lastModifiedMillis = lastModified;
            return true;
        }
        return false;
    }

    @Override
    public List<ConfigurationListener> getListeners() {
        if (super.getListeners() != null) {
            return Collections.unmodifiableList(super.getListeners());
        } else {
            return null;
        }
    }

    @Override
    public void modified() {
        if (getListeners() != null) {
            super.modified();
        }
        fileModified(getSource().getFile());
        lastModifiedMillis = getSource().getFile().lastModified();
    }

    @Override
    public void watching(Source source) {
        lastModifiedMillis = source.getFile().lastModified();
        super.watching(source);
    }

    @Override
    public Watcher newWatcher(final Reconfigurable reconfigurable, final List<ConfigurationListener> listeners,
        long lastModifiedMillis) {
        WrappedFileWatcher watcher = new WrappedFileWatcher(this.watcher, getConfiguration(), reconfigurable, listeners,
            lastModifiedMillis);
        if (getSource() != null) {
            watcher.watching(getSource());
        }
        return watcher;
    }
}
