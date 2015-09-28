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
public class Script {

    private static final Logger logger = StatusLogger.getLogger();

    private final String language;
    private final String scriptText;
    private final String name;

    public Script(final String name, final String language, final String body) {
        this.language = language;
        this.scriptText = body;
        this.name = name == null ? this.toString() : name;
    }

    public String getLanguage() {
        return this.language;
    }

    public String getScriptText() {
        return this.scriptText;
    }

    public String getName() {
        return this.name;
    }


    @PluginFactory
    public static Script createScript(@PluginAttribute("name") final String name,
                                      @PluginAttribute("language") String language,
                                      @PluginValue("scriptText") final String scriptText) {
        if (language == null) {
            logger.info("No script language supplied, defaulting to JavaScript");
            language = "JavaScript";
        }
        if (scriptText == null) {
            logger.error("No script provided");
            return null;
        }
        return new Script(name, language, scriptText);

    }
}
