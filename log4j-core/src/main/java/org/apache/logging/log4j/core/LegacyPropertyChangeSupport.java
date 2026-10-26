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
package org.apache.logging.log4j.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.core.config.Configuration;

/**
 * Backs the deprecated {@code java.beans} listener methods of {@link LoggerContext}.
 * <p>
 *   This class is the only place that touches {@code java.beans} types at runtime, and {@link LoggerContext} creates
 *   it lazily. A runtime without the {@code java.desktop} module therefore only fails if a caller registers a
 *   {@link PropertyChangeListener}.
 * </p>
 */
final class LegacyPropertyChangeSupport {

    private final List<PropertyChangeListener> listeners = new CopyOnWriteArrayList<>();

    void add(final PropertyChangeListener listener) {
        listeners.add(listener);
    }

    void remove(final PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    void fireConfigurationChange(
            final LoggerContext source, final Configuration oldConfiguration, final Configuration newConfiguration) {
        final PropertyChangeEvent event =
                new PropertyChangeEvent(source, LoggerContext.PROPERTY_CONFIG, oldConfiguration, newConfiguration);
        for (final PropertyChangeListener listener : listeners) {
            listener.propertyChange(event);
        }
    }
}
