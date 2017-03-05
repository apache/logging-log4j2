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
package org.apache.logging.log4j.core.script;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;

/**
 * Container for the language and body of a script.
 */
@Plugin(name = "Script", category = Node.CATEGORY, printObject = true)
public class Script extends AbstractScript {

    public Script(final String name, final String language, final String scriptText) {
        super(name, language, scriptText);
    }

    @PluginFactory
    public static Script createScript(
            // @formatter:off
            @PluginAttribute("name") final String name,
            @PluginAttribute("language") String language,
            @PluginValue("scriptText") final String scriptText) {
            // @formatter:on
        if (language == null) {
            LOGGER.info("No script language supplied, defaulting to {}", DEFAULT_LANGUAGE);
            language = DEFAULT_LANGUAGE;
        }
        if (scriptText == null) {
            LOGGER.error("No scriptText attribute provided for ScriptFile {}", name);
            return null;
        }
        return new Script(name, language, scriptText);

    }

    @Override
    public String toString() {
        return getName() != null ? getName() : super.toString();
    }
}
