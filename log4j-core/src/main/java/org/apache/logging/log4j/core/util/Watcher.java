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
package org.apache.logging.log4j.core.util;

import java.io.File;
import java.util.List;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.Reconfigurable;

/**
 * Watches for changes in a Source and performs an action when it is modified.
 *
 * @see WatchManager
 * @deprecated Use {@link org.apache.logging.log4j.config.spi.Watcher} from {@code config-spi} for the
 *             portable contract. This interface adds core configuration integration types.
 */
@Deprecated
public interface Watcher extends org.apache.logging.log4j.config.spi.Watcher {

    String CATEGORY = "Watcher";
    String ELEMENT_TYPE = "watcher";

    /**
     * Returns the list of listeners for this configuration.
     * @return The list of listeners.
     */
    List<ConfigurationListener> getListeners();

    /**
     * Called when the configuration has been modified.
     */
    void modified();

    /**
     * Periodically called to determine if the configuration has been modified.
     * @return true if the configuration was modified, false otherwise.
     */
    boolean isModified();

    /**
     * Returns the time the source was last modified or 0 if it is not available.
     * @return the time the source was last modified.
     */
    @Override
    long getLastModified();

    /**
     * Called when the Watcher is registered.
     * @param source the Source that is being watched.
     */
    void watching(Source source);

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #watching(Source)} using the core {@link Source} facade.
     */
    @Override
    default void watching(final org.apache.logging.log4j.common.util.Source source) {
        watching(Source.fromCommon(source));
    }

    /**
     * Default implementation notifies listeners when only a {@link File} change is detected.
     */
    @Override
    default void fileModified(final File file) {
        modified();
    }

    /**
     * Returns the Source being monitored.
     * @return the Source.
     */
    Source getSource();

    /**
     * Creates a new Watcher by copying the original and using the new Reconfigurable and listeners.
     * @param reconfigurable The Reconfigurable.
     * @param listeners the listeners.
     * @param lastModifiedMillis The time the resource was last modified in milliseconds.
     * @return A new Watcher.
     */
    Watcher newWatcher(Reconfigurable reconfigurable, List<ConfigurationListener> listeners, long lastModifiedMillis);
}
