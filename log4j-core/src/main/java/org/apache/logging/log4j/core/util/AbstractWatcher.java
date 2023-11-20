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

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Reconfigurable;

/**
 * Watcher for configuration files. Causes a reconfiguration when a file changes.
 */
public abstract class AbstractWatcher implements Watcher {

    private final Reconfigurable reconfigurable;
    private final List<Consumer<Reconfigurable>> reconfigurableListeners;
    private final Executor executor;
    private final Configuration configuration;
    private Source source;

    public AbstractWatcher(
            final Configuration configuration,
            final Reconfigurable reconfigurable,
            final List<Consumer<Reconfigurable>> reconfigurableListeners) {
        this.configuration = configuration;
        this.reconfigurable = reconfigurable;
        this.reconfigurableListeners = reconfigurableListeners;
        this.executor = reconfigurableListeners != null
                ? Executors.newCachedThreadPool(
                        Log4jThreadFactory.createDaemonThreadFactory("ConfigurationFileWatcher"))
                : null;
    }

    @Override
    public List<Consumer<Reconfigurable>> getListeners() {
        return reconfigurableListeners;
    }

    @Override
    public void modified() {
        for (final Consumer<Reconfigurable> reconfigurableListener : reconfigurableListeners) {
            executor.execute(() -> reconfigurableListener.accept(reconfigurable));
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public abstract long getLastModified();

    public abstract boolean isModified();

    @Override
    public void watching(final Source source) {
        this.source = source;
    }

    @Override
    public Source getSource() {
        return source;
    }
}
