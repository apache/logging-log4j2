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
package org.apache.logging.log4j.core.appender;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

@Plugin(name = "Hanging", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class HangingAppender extends AbstractAppender {

    private static final long serialVersionUID = 1L;

    private final long delay;
    private final long startupDelay;
    private final long shutdownDelay;

    public HangingAppender(final String name, final long delay, final long startupDelay, final long shutdownDelay) {
        super(name, null, null);
        this.delay = delay;
        this.startupDelay = startupDelay;
        this.shutdownDelay = shutdownDelay;
    }

    @Override
    public void append(final LogEvent event) {
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException ignore) {
            // ignore
        }
    }

    @PluginFactory
    public static HangingAppender createAppender(
            @PluginAttribute("name")
            @Required(message = "No name provided for HangingAppender")
            final String name,
            @PluginAttribute("delay") final long delay,
            @PluginAttribute("startupDelay") final long startupDelay,
            @PluginAttribute("shutdownDelay") final long shutdownDelay,
            @PluginElement("Layout") final Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter) {
        return new HangingAppender(name, delay, startupDelay, shutdownDelay);
    }

    @Override
    public void start() {
        try {
            Thread.sleep(startupDelay);
        } catch (final InterruptedException ignore) {
            // ignore
        }
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        super.stop(timeout, timeUnit, false);
        try {
            Thread.sleep(shutdownDelay);
        } catch (final InterruptedException ignore) {
            // ignore
        }
        setStopped();
        return true;
    }
}
