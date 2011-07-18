package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.PluginManager;
import org.apache.logging.log4j.core.config.plugins.PluginType;
import org.apache.logging.log4j.internal.StatusLogger;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * ConfigurationFactory allows the configuration implementation to be dynamically chosen in 1
 * of 3 ways:
 * 1. A system property named "log4j.configurationFactory" can be set with the name of the
 * ConfigurationFactory to be used.
 * 2. setConfigurationFactory can be called with the instance of the ConfigurationFactory to
 * be used. This must be called before any other calls to Log4j.
 * 3. A ConfigurationFactory implementation can be added to the classpath and configured as a
 * plugin. The Order annotation should be used to configure the factory to be the first one
 * inspected. See XMLConfigurationFactory for an example.
 *
 * If the ConfigurationFactory that was added returns null on a call to getConfiguration the
 * any other ConfigurationFactories found as plugins will be called in their respective order.
 * DefaultConfiguration is always called last if no configuration has been returned.
 */
public abstract class ConfigurationFactory {

    public static final String CONFIGURATION_FACTORY_PROPERTY = "log4j.configurationFactory";

    private static List<ConfigurationFactory> factories = new ArrayList<ConfigurationFactory>();

    private static Logger logger = StatusLogger.getLogger();

    public static ConfigurationFactory getInstance() {
        String factoryClass = System.getProperty(CONFIGURATION_FACTORY_PROPERTY);
        if (factoryClass != null) {
            addFactory(factoryClass);
        }
        PluginManager manager = new PluginManager("ConfigurationFactory");
        manager.collectPlugins();
        Map<String, PluginType> plugins = manager.getPlugins();
        Set<WeightedFactory> ordered = new TreeSet<WeightedFactory>();
        for (PluginType type : plugins.values()) {
            try {
                Class<ConfigurationFactory> clazz = type.getPluginClass();
                Order o = clazz.getAnnotation(Order.class);
                Integer weight = o.value();
                if (o != null) {
                    ordered.add(new WeightedFactory(weight, clazz));
                }
            } catch (Exception ex) {

            }
        }
        for (WeightedFactory wf : ordered) {
            addFactory(wf.factoryClass);
        }
        return new Factory();
    }

    private static void addFactory(String factoryClass) {
        try {
            Class clazz = Class.forName(factoryClass);
            addFactory(clazz);
        } catch (ClassNotFoundException ex) {
            logger.error("Unable to load class " + factoryClass, ex);
        } catch (Exception ex) {
            logger.error("Unable to load class " + factoryClass, ex);
        }
    }

    private static void addFactory(Class factoryClass) {
        try {
            factories.add((ConfigurationFactory) factoryClass.newInstance());
        } catch (Exception ex) {
            logger.error("Unable to create instance of " + factoryClass.getName(), ex);
        }
    }

    public static void setConfigurationFactory(ConfigurationFactory factory) {
        factories.add(0, factory);
    }

    public static void removeConfigurationFactory(ConfigurationFactory factory) {
        factories.remove(factory);
    }

    public abstract Configuration getConfiguration(String name, URI configLocation);

    private static class WeightedFactory implements Comparable<WeightedFactory> {
        private int weight;
        private Class<ConfigurationFactory> factoryClass;

        public WeightedFactory(int weight, Class<ConfigurationFactory> clazz) {
            this.weight = weight;
            this.factoryClass = clazz;
        }

        public int compareTo(WeightedFactory wf) {
            int w = wf.weight;
            if (weight == w) {
                return 0;
            } else if (weight > w) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private static class Factory extends ConfigurationFactory {

        public Configuration getConfiguration(String name, URI configLocation) {

            for (ConfigurationFactory factory : factories) {
                Configuration c = factory.getConfiguration(name, configLocation);
                if (c != null) {
                    return c;
                }
            }
            return new DefaultConfiguration();
        }
    }
}
