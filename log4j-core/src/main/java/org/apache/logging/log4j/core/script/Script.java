package org.apache.logging.log4j.core.script;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Container for the language and body of a script.
 */
@Plugin(name = "Script", category = Node.CATEGORY, printObject = true)
public class Script extends AbstractScript {

    private static final Logger logger = StatusLogger.getLogger();

    public Script(String name, String language, String scriptText) {
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
            logger.info("No script language supplied, defaulting to {}", DEFAULT_LANGUAGE);
            language = DEFAULT_LANGUAGE;
        }
        if (scriptText == null) {
            logger.error("No scriptText attribute provided for ScriptFile {}", name);
            return null;
        }
        return new Script(name, language, scriptText);

    }

    @Override
    public String toString() {
        return getName() != null ? getName() : super.toString();
    }
}
