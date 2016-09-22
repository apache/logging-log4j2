package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * An Appender that ignores log events. Use for compatibility with version 1.2.
 */
@Plugin(name = NullAppender.PLUGIN_NAME, category = "Core", elementType = "appender", printObject = true)
public class NullAppender extends AbstractAppender {

    public static final String PLUGIN_NAME = "Null";

    @PluginFactory
    public static NullAppender createAppender(@PluginAttribute("name") final String name) {
        return new NullAppender(name);
    }

    private NullAppender(final String name) {
        super(name, null, null);
        // Do nothing
    }

    @Override
    public void append(LogEvent event) {
        // Do nothing
    }

}
