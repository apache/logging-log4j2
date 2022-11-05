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
package org.apache.logging.log4j.script.factory;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.script.ScriptManager;
import org.apache.logging.log4j.core.script.ScriptManagerFactory;
import org.apache.logging.log4j.core.util.WatchManager;
import org.apache.logging.log4j.script.ScriptManagerImpl;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Creates a ScriptManager.
 */
public class ScriptManagerFactoryImpl implements ScriptManagerFactory {
    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Control which script languages are allowed, if any.
     */
    public static final String SCRIPT_LANGUAGES = "log4j2.Script.enableLanguages";

    @Override
    public ScriptManager createScriptManager(Configuration configuration, WatchManager watchManager) {
        String scriptLanguages = PropertiesUtil.getProperties().getStringProperty(SCRIPT_LANGUAGES);
        if (scriptLanguages != null) {
            try {
                return new ScriptManagerImpl(configuration, watchManager);
            } catch (final LinkageError | Exception e) {
                // LOG4J2-1920 ScriptEngineManager is not available in Android
                LOGGER.info("Cannot initialize scripting support because this JRE does not support it.", e);
            }
        }
        return null;
    }
}
