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

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.script.AbstractScript;
import org.apache.logging.log4j.util.Strings;

/**
 * A  container of Scripts.
 */
@Plugin(name = "Scripts", category = Core.CATEGORY_NAME)
public final class ScriptsPlugin {

    private ScriptsPlugin() {}

    /**
     * Return the array of scripts
     * @param scripts An array of Scripts.
     * @return The array of AbstractScripts.
     */
    @PluginFactory
    public static AbstractScript[] createScripts(@PluginElement("Scripts") final AbstractScript[] scripts) {
        if (scripts == null || scripts.length == 0) {
            return scripts;
        }

        final List<AbstractScript> validScripts = new ArrayList<>(scripts.length);
        for (final AbstractScript script : scripts) {
            if (Strings.isBlank(script.getName())) {
                throw new ConfigurationException("A script defined in <Scripts> lacks an explicit 'name' attribute");
            } else {
                validScripts.add(script);
            }
        }
        return validScripts.toArray(new AbstractScript[0]);
    }
}
