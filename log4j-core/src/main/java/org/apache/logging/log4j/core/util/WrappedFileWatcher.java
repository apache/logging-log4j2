/*
 * Copyright (c) 2019 Nextiva, Inc. to Present.
 * All rights reserved.
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
