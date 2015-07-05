package org.apache.logging.log4j.configuration;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

import java.net.URI;

/**
 * Factory to construct a  CustomConfiguration.
 */
@Plugin(name = "CustomConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(50)
public class CustomConfigurationFactory extends ConfigurationFactory {

    /**
     * Valid file extensions for XML files.
     */
    public static final String[] SUFFIXES = new String[] {"*"};

    /**
     * Returns the Configuration.
     * @param source The InputSource.
     * @return The Configuration.
     */
    @Override
    public Configuration getConfiguration(final ConfigurationSource source) {
        return new CustomConfiguration(source);
    }

    @Override
    public Configuration getConfiguration(final String name, final URI configLocation) {
        return new CustomConfiguration();
    }

    /**
     * Returns the file suffixes for XML files.
     * @return An array of File extensions.
     */
    @Override
    public String[] getSupportedTypes() {
        return SUFFIXES;
    }
}
