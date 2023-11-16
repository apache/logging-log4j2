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
package org.apache.logging.log4j.core.script;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;

/**
 * Container for the language and body of a script.
 */
@Plugin(name = Script.PLUGIN_NAME, category = Node.CATEGORY, printObject = true)
public class Script extends AbstractScript {

    private static final String ATTR_LANGUAGE = "language";
    private static final String ATTR_SCRIPT_TEXT = "scriptText";
    static final String PLUGIN_NAME = "Script";

    public Script(final String name, final String language, final String scriptText) {
        super(name, language, scriptText);
    }

    @PluginFactory
    public static Script createScript(
            // @formatter:off
            @PluginAttribute("name") final String name,
            @PluginAttribute(ATTR_LANGUAGE) String language,
            @PluginValue(ATTR_SCRIPT_TEXT) final String scriptText) {
        // @formatter:on
        if (language == null) {
            LOGGER.error("No '{}' attribute provided for {} plugin '{}'", ATTR_LANGUAGE, PLUGIN_NAME, name);
            language = DEFAULT_LANGUAGE;
        }
        if (scriptText == null) {
            LOGGER.error("No '{}' attribute provided for {} plugin '{}'", ATTR_SCRIPT_TEXT, PLUGIN_NAME, name);
            return null;
        }
        return new Script(name, language, scriptText);
    }

    @Override
    public String toString() {
        return getName() != null ? getName() : super.toString();
    }
}
