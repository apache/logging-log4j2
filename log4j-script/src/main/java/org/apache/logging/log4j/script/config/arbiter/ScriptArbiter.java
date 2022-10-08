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
package org.apache.logging.log4j.script.config.arbiter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.arbiters.Arbiter;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginNode;
import org.apache.logging.log4j.core.script.Script;
import org.apache.logging.log4j.core.script.ScriptBindings;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.script.ScriptManagerImpl;
import org.apache.logging.log4j.script.ScriptRef;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Condition that evaluates a script.
 */
@Configurable(elementType = Arbiter.ELEMENT_TYPE, deferChildren = true, printObject = true)
@Plugin
public class ScriptArbiter implements Arbiter {

    private final Script script;
    private final Configuration configuration;

    private ScriptArbiter(final Configuration configuration, final Script script) {
        this.configuration = configuration;
        this.script = script;
    }

    /**
     * Returns the boolean result of the Script.
     */
    @Override
    public boolean isCondition() {
        final ScriptBindings bindings = ScriptManagerImpl.createBindings();
        bindings.putAll(configuration.getProperties());
        bindings.put("substitutor", configuration.getStrSubstitutor());
        final Object object = configuration.getScriptManager().execute(script.getName(), bindings);
        return Boolean.parseBoolean(object.toString());
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ScriptArbiter> {

        private static final Logger LOGGER = StatusLogger.getLogger();

        @PluginConfiguration
        private AbstractConfiguration configuration;

        @PluginNode
        private Node node;

        public Builder setConfiguration(final AbstractConfiguration configuration) {
            this.configuration = configuration;
            return asBuilder();
        }

        public Builder setNode(final Node node) {
            this.node = node;
            return asBuilder();
        }

        public Builder asBuilder() {
            return this;
        }

        public ScriptArbiter build() {
            Script script = null;
            for (final Node child : node.getChildren()) {
                final PluginType<?> type = child.getType();
                if (type == null) {
                    LOGGER.error("Node {} is missing a Plugintype", child.getName());
                    continue;
                }
                if (Script.class.isAssignableFrom(type.getPluginClass())) {
                    script = (Script) configuration.createPluginObject(child);
                    node.getChildren().remove(child);
                    break;
                }
            }

            if (script == null) {
                LOGGER.error("A Script, ScriptFile or ScriptRef element must be provided for this ScriptFilter");
                return null;
            }
            if (configuration.getScriptManager() == null) {
                LOGGER.error("Script support is not enabled");
                return null;
            }
            if (script instanceof ScriptRef) {
                if (configuration.getScriptManager().getScript(script.getName()) == null) {
                    LOGGER.error("No script with name {} has been declared.", script.getName());
                    return null;
                }
            } else {
                if (!configuration.getScriptManager().addScript(script)) {
                    return null;
                }
            }
            return new ScriptArbiter(configuration, script);
        }
    }
}
