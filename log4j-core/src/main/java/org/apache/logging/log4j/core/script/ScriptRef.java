package org.apache.logging.log4j.core.script;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Container for the language and body of a script.
 */
@Plugin(name = "ScriptRef", category = Node.CATEGORY, printObject = true)
public class ScriptRef extends AbstractScript {

    private static final Logger logger = StatusLogger.getLogger();
    private final ScriptManager scriptManager;

    public ScriptRef(String name, ScriptManager scriptManager) {
        super(name, null, null);
        this.scriptManager = scriptManager;
    }

    @Override
    public String getLanguage() {
        AbstractScript script = this.scriptManager.getScript(getName());
        return script != null ? script.getLanguage() : null;
    }


    @Override
    public String getScriptText() {
        AbstractScript script = this.scriptManager.getScript(getName());
        return script != null ? script.getScriptText() : null;
    }

    @PluginFactory
    public static ScriptRef createReference(
            // @formatter:off
            @PluginAttribute("ref") final String name,
            @PluginConfiguration Configuration configuration) {
            // @formatter:on
        if (name == null) {
            logger.error("No script name provided");
            return null;
        }
        return new ScriptRef(name, configuration.getScriptManager());

    }
}
