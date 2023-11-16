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
package org.apache.logging.log4j.core.appender;

import java.io.Serializable;
import java.util.Objects;
import javax.script.Bindings;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.script.AbstractScript;
import org.apache.logging.log4j.core.script.ScriptManager;

@Plugin(
        name = "ScriptAppenderSelector",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE,
        printObject = true)
public class ScriptAppenderSelector extends AbstractAppender {

    /**
     * Builds an appender.
     */
    public static final class Builder implements org.apache.logging.log4j.core.util.Builder<Appender> {

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
                LOGGER.error("Name missing.");
                return null;
            }
            if (script == null) {
                LOGGER.error("Script missing for ScriptAppenderSelector appender {}", name);
                return null;
            }
            if (appenderSet == null) {
                LOGGER.error("AppenderSet missing for ScriptAppenderSelector appender {}", name);
                return null;
            }
            if (configuration == null) {
                LOGGER.error("Configuration missing for ScriptAppenderSelector appender {}", name);
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
            final Bindings bindings = scriptManager.createBindings(script);
            LOGGER.debug(
                    "ScriptAppenderSelector '{}' executing {} '{}': {}",
                    name,
                    script.getLanguage(),
                    script.getName(),
                    script.getScriptText());
            final Object object = scriptManager.execute(script.getName(), bindings);
            final String actualAppenderName = Objects.toString(object, null);
            LOGGER.debug("ScriptAppenderSelector '{}' selected '{}'", name, actualAppenderName);
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

        public Builder withAppenderNodeSet(@SuppressWarnings("hiding") final AppenderSet appenderSet) {
            this.appenderSet = appenderSet;
            return this;
        }

        public Builder withConfiguration(@SuppressWarnings("hiding") final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder withName(@SuppressWarnings("hiding") final String name) {
            this.name = name;
            return this;
        }

        public Builder withScript(@SuppressWarnings("hiding") final AbstractScript script) {
            this.script = script;
            return this;
        }
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private ScriptAppenderSelector(
            final String name,
            final Filter filter,
            final Layout<? extends Serializable> layout,
            final Property[] properties) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(final LogEvent event) {
        // Do nothing: This appender is only used to discover and build another appender
    }
}
