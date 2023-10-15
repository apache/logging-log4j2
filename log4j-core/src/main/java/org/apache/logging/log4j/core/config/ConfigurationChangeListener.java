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

import java.util.EventListener;

import org.apache.logging.log4j.core.LoggerContext;

/**
 * Event listener for being notified from a {@link LoggerContext} that a {@link Configuration} is being replaced.
 * In Log4j 2.x, this was handled via the {@code java.beans.PropertyChangeListener} interface, but this was
 * extracted into the {@code java.desktop} module in Java 9.
 *
 * @since 3.0.0
 */
@FunctionalInterface
public interface ConfigurationChangeListener extends EventListener {
    void onChange(final ConfigurationChangeEvent event);
}
