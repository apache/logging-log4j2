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

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.FileWatcher;
import org.apache.logging.log4j.core.util.WatchManager;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Manages the scripts use by the Configuration.
 */
public class ScriptManager implements FileWatcher, Serializable {
    
    private abstract class AbstractScriptRunner implements ScriptRunner {
        
        private static final String KEY_STATUS_LOGGER = "statusLogger";
        private static final String KEY_CONFIGURATION = "configuration";
        
        @Override
        public Bindings createBindings() {
            final SimpleBindings bindings = new SimpleBindings();
            bindings.put(KEY_CONFIGURATION, configuration);
            bindings.put(KEY_STATUS_LOGGER, logger);
            return bindings;
        }

    }

    private static final long serialVersionUID = -2534169384971965196L;
    private static final String KEY_THREADING = "THREADING";
    private static final Logger logger = StatusLogger.getLogger();
    
    private final Configuration configuration;
    private final ScriptEngineManager manager = new ScriptEngineManager();
    private final ConcurrentMap<String, ScriptRunner> scriptRunners = new ConcurrentHashMap<>();
    private final String languages;
    private final WatchManager watchManager;

    public ScriptManager(final Configuration configuration, final WatchManager watchManager) {
        this.configuration = configuration;
        this.watchManager = watchManager;
        final List<ScriptEngineFactory> factories = manager.getEngineFactories();
        if (logger.isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder();
            logger.debug("Installed script engines");
            for (final ScriptEngineFactory factory : factories) {
                String threading = (String) factory.getParameter(KEY_THREADING);
                if (threading == null) {
                    threading = "Not Thread Safe";
                }
                final StringBuilder names = new StringBuilder();
                for (final String name : factory.getNames()) {
                    if (names.length() > 0) {
                        names.append(", ");
                    }
                    names.append(name);
                }
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(names);
                final boolean compiled = factory.getScriptEngine() instanceof Compilable;
                logger.debug(factory.getEngineName() + " Version: " + factory.getEngineVersion() +
                    ", Language: " + factory.getLanguageName() + ", Threading: " + threading +
                    ", Compile: " + compiled + ", Names: {" + names.toString() + "}");
            }
            languages = sb.toString();
        } else {
            final StringBuilder names = new StringBuilder();
            for (final ScriptEngineFactory factory : factories) {
                for (final String name : factory.getNames()) {
                    if (names.length() > 0) {
                        names.append(", ");
                    }
                    names.append(name);
                }
            }
            languages = names.toString();
        }
    }

    public void addScript(final AbstractScript script) {
        final ScriptEngine engine = manager.getEngineByName(script.getLanguage());
        if (engine == null) {
            logger.error("No ScriptEngine found for language " + script.getLanguage() + ". Available languages are: " +
                    languages);
            return;
        }
        if (engine.getFactory().getParameter(KEY_THREADING) == null) {
            scriptRunners.put(script.getName(), new ThreadLocalScriptRunner(script));
        } else {
            scriptRunners.put(script.getName(), new MainScriptRunner(engine, script));
        }

        if (script instanceof ScriptFile) {
            final ScriptFile scriptFile = (ScriptFile) script;
            final Path path = scriptFile.getPath();
            if (scriptFile.isWatched() && path != null) {
                watchManager.watchFile(path.toFile(), this);
            }
        }
    }

    public Bindings createBindings(final AbstractScript script) {
        return getScriptRunner(script).createBindings();
    }

    public AbstractScript getScript(final String name) {
        final ScriptRunner runner = scriptRunners.get(name);
        return runner != null ? runner.getScript() : null;
    }

    @Override
    public void fileModified(final File file) {
        final ScriptRunner runner = scriptRunners.get(file.toString());
        if (runner == null) {
            logger.info("{} is not a running script");
            return;
        }
        final ScriptEngine engine = runner.getScriptEngine();
        final AbstractScript script = runner.getScript();
        if (engine.getFactory().getParameter(KEY_THREADING) == null) {
            scriptRunners.put(script.getName(), new ThreadLocalScriptRunner(script));
        } else {
            scriptRunners.put(script.getName(), new MainScriptRunner(engine, script));
        }

    }

    public Object execute(final String name, final Bindings bindings) {
        final ScriptRunner scriptRunner = scriptRunners.get(name);
        if (scriptRunner == null) {
            logger.warn("No script named {} could be found");
            return null;
        }
        return AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                return scriptRunner.execute(bindings);
            }
        });
    }

    private interface ScriptRunner {

        Bindings createBindings();
        
        Object execute(Bindings bindings);

        AbstractScript getScript();

        ScriptEngine getScriptEngine();
    }

    private class MainScriptRunner extends AbstractScriptRunner {
        private final AbstractScript script;
        private final CompiledScript compiledScript;
        private final ScriptEngine scriptEngine;

        public MainScriptRunner(final ScriptEngine scriptEngine, final AbstractScript script) {
            this.script = script;
            this.scriptEngine = scriptEngine;
            CompiledScript compiled = null;
            if (scriptEngine instanceof Compilable) {
                logger.debug("Script {} is compilable", script.getName());
                compiled = AccessController.doPrivileged(new PrivilegedAction<CompiledScript>() {
                    @Override
                    public CompiledScript run() {
                        try {
                            return ((Compilable) scriptEngine).compile(script.getScriptText());
                        } catch (final Throwable ex) {
                                /* ScriptException is what really should be caught here. However, beanshell's
                                 * ScriptEngine implements Compilable but then throws Error when the compile method
                                 * is called!
                                 */
                            logger.warn("Error compiling script", ex);
                            return null;
                        }
                    }
                });
            }
            compiledScript = compiled;
        }

        @Override
        public ScriptEngine getScriptEngine() {
            return this.scriptEngine;
        }

        @Override
        public Object execute(final Bindings bindings) {
            if (compiledScript != null) {
                try {
                    return compiledScript.eval(bindings);
                } catch (final ScriptException ex) {
                    logger.error("Error running script " + script.getName(), ex);
                    return null;
                }
            }
            try {
                return scriptEngine.eval(script.getScriptText(), bindings);
            }   catch (final ScriptException ex) {
                logger.error("Error running script " + script.getName(), ex);
                return null;
            }
        }

        @Override
        public AbstractScript getScript() {
            return script;
        }
    }

    private class ThreadLocalScriptRunner extends AbstractScriptRunner {
        private final AbstractScript script;

        private final ThreadLocal<MainScriptRunner> runners = new ThreadLocal<MainScriptRunner>() {
            @Override protected MainScriptRunner initialValue() {
                final ScriptEngine engine = manager.getEngineByName(script.getLanguage());
                return new MainScriptRunner(engine, script);
            }
        };

        public ThreadLocalScriptRunner(final AbstractScript script) {
            this.script = script;
        }

        @Override
        public Object execute(final Bindings bindings) {
            return runners.get().execute(bindings);
        }

        @Override
        public AbstractScript getScript() {
            return script;
        }
       @Override
        public ScriptEngine getScriptEngine() {
            return runners.get().getScriptEngine();
        }
    }

    private ScriptRunner getScriptRunner(final AbstractScript script) {
        return scriptRunners.get(script.getName());
    }
}
