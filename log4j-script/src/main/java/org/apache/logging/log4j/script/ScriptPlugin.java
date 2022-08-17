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

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.PluginValue;

/**
 * Container for the language and body of a script.
 */
@Configurable(printObject = true)
@Plugin(ScriptPlugin.PLUGIN_NAME)
public class ScriptPlugin extends AbstractScript {

    private static final String ATTR_LANGUAGE = "language";
    private static final String ATTR_SCRIPT_TEXT = "scriptText";
    static final String PLUGIN_NAME = "Script";

    public ScriptPlugin(final String name, final String language, final String scriptText) {
        super(name, language, scriptText);
    }

    @PluginFactory
    public static ScriptPlugin createScript(
            // @formatter:off
            @PluginAttribute final String name,
            @PluginAttribute String language,
            @PluginValue final String scriptText) {
            // @formatter:on
        if (language == null) {
            LOGGER.error("No '{}' attribute provided for {} plugin '{}'", ATTR_LANGUAGE, PLUGIN_NAME, name);
            language = DEFAULT_LANGUAGE;
        }
        if (scriptText == null) {
            LOGGER.error("No '{}' attribute provided for {} plugin '{}'", ATTR_SCRIPT_TEXT, PLUGIN_NAME, name);
            return null;
        }
        return new ScriptPlugin(name, language, scriptText);

    }

    @Override
    public String toString() {
        return getName() != null ? getName() : super.toString();
    }
}
