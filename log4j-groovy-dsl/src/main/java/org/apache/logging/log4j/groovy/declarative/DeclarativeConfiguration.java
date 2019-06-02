package org.apache.logging.log4j.groovy.declarative;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.ConfiguratonFileWatcher;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.groovy.api.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class DeclarativeConfiguration extends AbstractConfiguration implements Reconfigurable {

    private final Component root;

    DeclarativeConfiguration(final LoggerContext context, final ConfigurationSource source, final Component root) {
        super(context, source);
        this.root = root;
        rootNode.getAttributes().putAll(root.getAttributes());

        // pre-configure status logger
        StatusConfiguration statusConfig = new StatusConfiguration().setStatus(getDefaultStatus());
        Map<String, String> attributes = getRootNode().getAttributes();
        if (attributes.containsKey("dest")) {
            statusConfig.setDestination(attributes.get("dest"));
        }
        if (attributes.containsKey("status")) {
            statusConfig.setStatus(Level.getLevel(attributes.get("status")));
        }
        if (attributes.containsKey("verbose")) {
            statusConfig.setVerbosity(attributes.get("verbose"));
        }
        statusConfig.initialize();

        // set up simple attributes, too
        if (attributes.containsKey("packages")) {
            pluginPackages.addAll(Arrays.asList(attributes.get("packages").split(Patterns.COMMA_SEPARATOR)));
        }
        isShutdownHookEnabled = !"disable".equalsIgnoreCase(attributes.get("shutdownHook"));
        if (attributes.containsKey("shutdownTimeout")) {
            shutdownTimeoutMillis = Long.parseLong(attributes.get("shutdownTimeout"));
        }
    }

    @Override
    public void setup() {
        Map<String, String> attributes = getRootNode().getAttributes();
        if (attributes.containsKey("advertiser")) {
            try {
                createAdvertiser();
            } catch (IOException e) {
                LOGGER.warn("Cannot create advertiser", e);
            }
        }
        if (attributes.containsKey("monitorInterval")) {
            int monitorInterval = Integer.parseInt(attributes.get("monitorInterval"));
            if (monitorInterval > 0 && getConfigurationSource().getFile() != null) {
                getWatchManager().setIntervalSeconds(monitorInterval);
                getWatchManager().watchFile(getConfigurationSource().getFile(), new ConfiguratonFileWatcher(this, listeners));
            }
        }
        if (attributes.containsKey("name")) {
            setName(attributes.get("name"));
        }
        for (Component component : root.getComponents()) {
            getRootNode().getChildren().add(component.toNode(getRootNode(), getPluginManager()));
        }
    }

    private void createAdvertiser() throws IOException {
        byte[] buffer = toByteArray(getConfigurationSource().getInputStream());
        Map<String, String> attributes = getRootNode().getAttributes();
        createAdvertiser(attributes.get("advertiser"), getConfigurationSource(), buffer, attributes.getOrDefault("contentType", "text"));
    }

    @Override
    public Configuration reconfigure() {
        try {
            ConfigurationSource source = getConfigurationSource().resetInputStream();
            if (source == null) {
                return null;
            }
            DeclarativeConfigurationFactory factory = new DeclarativeConfigurationFactory();
            Configuration config = factory.getConfiguration(getLoggerContext(), source);
            return config == null || config.getState() != State.INITIALIZING ? null : config;
        } catch (IOException e) {
            LOGGER.error("Cannot locate file {}: {}", getConfigurationSource(), e);
            return null;
        }
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
