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
package org.apache.logging.log4j.script;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.script.Script;
import org.apache.logging.log4j.core.script.ScriptManager;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

/**
 * Contains a reference to a script defined elsewhere in the configuration.
 */
@Configurable(printObject = true)
@Plugin
public class ScriptRef extends AbstractScript {

    private final ScriptManager scriptManager;

    public ScriptRef(final String name, final ScriptManager scriptManager) {
        super(name, null, null);
        this.scriptManager = scriptManager;
    }

    @Override
    public String getLanguage() {
        final Script script = this.scriptManager.getScript(getName());
        return script != null ? script.getLanguage() : null;
    }


    @Override
    public String getScriptText() {
        final Script script = this.scriptManager.getScript(getName());
        return script != null ? script.getScriptText() : null;
    }

    @PluginFactory
    public static ScriptRef createReference(
            // @formatter:off
            @PluginAttribute("ref") final String name,
            @PluginConfiguration final Configuration configuration) {
            // @formatter:on
        if (name == null) {
            LOGGER.error("No script name provided");
            return null;
        }
        return new ScriptRef(name, configuration.getScriptManager());

    }

    @Override
    public String toString() {
        return "ref=" + getName();
    }
}
