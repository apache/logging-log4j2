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
package org.apache.logging.log4j.script.appender;

import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderSet;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.script.ScriptBindings;
import org.apache.logging.log4j.core.script.ScriptManager;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.script.AbstractScript;

import java.io.Serializable;
import java.util.Objects;

@Configurable(elementType = Appender.ELEMENT_TYPE, printObject = true)
@Plugin
public class ScriptAppenderSelector extends AbstractAppender {

    /**
     * Builds an appender.
     */
    public static final class Builder implements org.apache.logging.log4j.plugins.util.Builder<Appender> {

        @PluginElement("AppenderSet")
        @Required
        private AppenderSet appenderSet;

        @PluginConfiguration
        @Required
        private Configuration configuration;

        @PluginBuilderAttribute
        @Required
        private String name;

        @PluginElement("Script")
        @Required
        private AbstractScript script;

        @Override
        public Appender build() {
            if (name == null) {
                AbstractLifeCycle.LOGGER.error("Name missing.");
                return null;
            }
            if (script == null) {
                AbstractLifeCycle.LOGGER.error("Script missing for ScriptAppenderSelector appender {}", name);
                return null;
            }
            if (appenderSet == null) {
                AbstractLifeCycle.LOGGER.error("AppenderSet missing for ScriptAppenderSelector appender {}", name);
                return null;
            }
            if (configuration == null) {
                AbstractLifeCycle.LOGGER.error("Configuration missing for ScriptAppenderSelector appender {}", name);
                return null;
            }
            final ScriptManager scriptManager = configuration.getScriptManager();
            if (scriptManager == null) {
                LOGGER.error("Script support is not enabled");
                return null;
            }
            if (!scriptManager.addScript(script)) {
                return null;
            }
            final ScriptBindings bindings = scriptManager.createBindings(script);
            AbstractLifeCycle.LOGGER.debug("ScriptAppenderSelector '{}' executing {} '{}': {}", name, script.getLanguage(),
                    script.getName(), script.getScriptText());
            final Object object = scriptManager.execute(script.getName(), bindings);
            final String actualAppenderName = Objects.toString(object, null);
            AbstractLifeCycle.LOGGER.debug("ScriptAppenderSelector '{}' selected '{}'", name, actualAppenderName);
            return appenderSet.createAppender(actualAppenderName, name);
        }

        public AppenderSet getAppenderSet() {
            return appenderSet;
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public String getName() {
            return name;
        }

        public AbstractScript getScript() {
            return script;
        }

        public Builder setAppenderNodeSet(@SuppressWarnings("hiding") final AppenderSet appenderSet) {
            this.appenderSet = appenderSet;
            return this;
        }

        public Builder setConfiguration(@SuppressWarnings("hiding") final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder setName(@SuppressWarnings("hiding") final String name) {
            this.name = name;
            return this;
        }

        public Builder setScript(@SuppressWarnings("hiding") final AbstractScript script) {
            this.script = script;
            return this;
        }

    }

    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private ScriptAppenderSelector(final String name, final Filter filter, final Layout<? extends Serializable> layout) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(final LogEvent event) {
        // Do nothing: This appender is only used to discover and build another appender
    }

}
