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
package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.script.SimpleBindings;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.script.AbstractScript;
import org.apache.logging.log4j.core.script.ScriptFile;
import org.apache.logging.log4j.core.script.ScriptRef;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * A condition of the {@link DeleteAction} where a user-provided script selects the files to delete from a provided
 * list. The specified script may be a {@link org.apache.logging.log4j.core.script.Script}, a {@link ScriptFile} or a {@link ScriptRef}.
 *
 * @see #createCondition(AbstractScript, Configuration)
 */
@Plugin(name = "ScriptCondition", category = Core.CATEGORY_NAME, printObject = true)
public class ScriptCondition {
    private static Logger LOGGER = StatusLogger.getLogger();

    private final AbstractScript script;
    private final Configuration configuration;

    /**
     * Constructs a new ScriptCondition.
     *
     * @param script the script that can select files to delete
     * @param configuration configuration containing the StrSubstitutor passed to the script
     */
    public ScriptCondition(final AbstractScript script, final Configuration configuration) {
        this.script = Objects.requireNonNull(script, "script");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    /**
     * Executes the script
     *
     * @param basePath base directory for files to delete
     * @param candidates a list of paths, that can be deleted by the script
     * @return a list of paths selected to delete by the script execution
     */
    @SuppressWarnings("unchecked")
    public List<PathWithAttributes> selectFilesToDelete(
            final Path basePath, final List<PathWithAttributes> candidates) {
        final SimpleBindings bindings = new SimpleBindings();
        bindings.put("basePath", basePath);
        bindings.put("pathList", candidates);
        bindings.putAll(configuration.getProperties());
        bindings.put("configuration", configuration);
        bindings.put("substitutor", configuration.getStrSubstitutor());
        bindings.put("statusLogger", LOGGER);
        final Object object = configuration.getScriptManager().execute(script.getName(), bindings);
        return (List<PathWithAttributes>) object;
    }

    /**
     * Creates the ScriptCondition.
     *
     * @param script The script to run. This may be a {@link org.apache.logging.log4j.core.script.Script}, a {@link ScriptFile} or a {@link ScriptRef}. The
     *            script must return a {@code List<PathWithAttributes>}. When the script is executed, it is provided the
     *            following bindings:
     *            <ul>
     *            <li>basePath - the directory from where the {@link DeleteAction Delete} action started scanning for
     *            files to delete. Can be used to relativize the paths in the pathList.</li>
     *            <li>pathList - a {@code java.util.List} containing {@link org.apache.logging.log4j.core.appender.rolling.action.PathWithAttributes} objects. (The script is
     *            free to modify and return this list.)</li>
     *            <li>substitutor - a {@link org.apache.logging.log4j.core.lookup.StrSubstitutor} that can be used to look up variables embedded in the base
     *            dir or other properties
     *            <li>statusLogger - the {@link StatusLogger} that can be used to log events during script execution
     *            <li>any properties declared in the configuration</li>
     *            </ul>
     * @param configuration the configuration
     * @return A ScriptCondition.
     */
    @PluginFactory
    public static ScriptCondition createCondition(
            @PluginElement("Script") final AbstractScript script,
            @PluginConfiguration final Configuration configuration) {

        if (script == null) {
            LOGGER.error("A Script, ScriptFile or ScriptRef element must be provided for this ScriptCondition");
            return null;
        }
        if (configuration.getScriptManager() == null) {
            LOGGER.error("Script support is not enabled");
            return null;
        }
        if (script instanceof ScriptRef) {
            if (configuration.getScriptManager().getScript(script.getName()) == null) {
                LOGGER.error("ScriptCondition: No script with name {} has been declared.", script.getName());
                return null;
            }
        } else {
            if (!configuration.getScriptManager().addScript(script)) {
                return null;
            }
        }
        return new ScriptCondition(script, configuration);
    }
}
