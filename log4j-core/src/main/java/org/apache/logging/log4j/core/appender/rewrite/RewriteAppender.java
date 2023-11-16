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
package org.apache.logging.log4j.core.appender.rewrite;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.LocationAware;
import org.apache.logging.log4j.core.util.Booleans;

/**
 * This Appender allows the logging event to be manipulated before it is processed by other Appenders.
 */
@Plugin(name = "Rewrite", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class RewriteAppender extends AbstractAppender {

    private final Configuration config;
    private final ConcurrentMap<String, AppenderControl> appenders = new ConcurrentHashMap<>();
    private final RewritePolicy rewritePolicy;
    private final AppenderRef[] appenderRefs;

    private RewriteAppender(
            final String name,
            final Filter filter,
            final boolean ignoreExceptions,
            final AppenderRef[] appenderRefs,
            final RewritePolicy rewritePolicy,
            final Configuration config,
            final Property[] properties) {
        super(name, filter, null, ignoreExceptions, properties);
        this.config = config;
        this.rewritePolicy = rewritePolicy;
        this.appenderRefs = appenderRefs;
    }

    @Override
    public void start() {
        for (final AppenderRef ref : appenderRefs) {
            final String name = ref.getRef();
            final Appender appender = config.getAppender(name);
            if (appender != null) {
                final Filter filter =
                        appender instanceof AbstractAppender ? ((AbstractAppender) appender).getFilter() : null;
                appenders.put(name, new AppenderControl(appender, ref.getLevel(), filter));
            } else {
                LOGGER.error("Appender " + ref + " cannot be located. Reference ignored");
            }
        }
        super.start();
    }

    /**
     * Modifies the event and pass to the subordinate Appenders.
     * @param event The LogEvent.
     */
    @Override
    public void append(LogEvent event) {
        if (rewritePolicy != null) {
            event = rewritePolicy.rewrite(event);
        }
        for (final AppenderControl control : appenders.values()) {
            control.callAppender(event);
        }
    }

    /**
     * Creates a RewriteAppender.
     * @param name The name of the Appender.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param appenderRefs An array of Appender names to call.
     * @param config The Configuration.
     * @param rewritePolicy The policy to use to modify the event.
     * @param filter A Filter to filter events.
     * @return The created RewriteAppender.
     */
    @PluginFactory
    public static RewriteAppender createAppender(
            @PluginAttribute("name") final String name,
            @PluginAttribute("ignoreExceptions") final String ignore,
            @PluginElement("AppenderRef") final AppenderRef[] appenderRefs,
            @PluginConfiguration final Configuration config,
            @PluginElement("RewritePolicy") final RewritePolicy rewritePolicy,
            @PluginElement("Filter") final Filter filter) {

        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        if (name == null) {
            LOGGER.error("No name provided for RewriteAppender");
            return null;
        }
        if (appenderRefs == null) {
            LOGGER.error("No appender references defined for RewriteAppender");
            return null;
        }
        return new RewriteAppender(name, filter, ignoreExceptions, appenderRefs, rewritePolicy, config, null);
    }

    @Override
    public boolean requiresLocation() {
        for (final AppenderControl control : appenders.values()) {
            final Appender appender = control.getAppender();
            if (appender instanceof LocationAware && ((LocationAware) appender).requiresLocation()) {
                return true;
            }
        }
        return false;
    }
}
