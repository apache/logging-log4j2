package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.PluginManager;
import org.apache.logging.log4j.core.config.plugins.PluginType;
import org.apache.logging.log4j.core.helpers.FileUtils;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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

    public static final String CONFIGURATION_FILE_PROPERTY = "log4j.configurationFile";

    private static List<ConfigurationFactory> factories = new ArrayList<ConfigurationFactory>();

    protected static Logger logger = StatusLogger.getLogger();

    protected File configFile = null;

    protected static final String TEST_PREFIX = "log4j2-test";

    protected static final String DEFAULT_PREFIX = "log4j2";

    private static ConfigurationFactory configFactory = new Factory();

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
        return configFactory;
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
        configFactory = factory;
    }

    public static void resetConfigurationFactory() {
        configFactory = new Factory();
    }

    public static void removeConfigurationFactory(ConfigurationFactory factory) {
        factories.remove(factory);
    }

    protected abstract String[] getSupportedTypes();

    protected boolean isActive() {
        return true;
    }

    public abstract Configuration getConfiguration(InputSource source);

    public Configuration getConfiguration(String name, URI configLocation) {
        if (!isActive()) {
            return null;
        }
        if (configLocation != null) {
            InputSource source = getInputFromURI(configLocation);
            if (source != null) {
                return getConfiguration(source);
            }
        }
        return null;
    }

    protected InputSource getInputFromURI(URI configLocation) {
        configFile = FileUtils.fileFromURI(configLocation);
        if (configFile != null && configFile.exists() && configFile.canRead()) {
            try {
                InputSource source = new InputSource(new FileInputStream(configFile));
                source.setSystemId(configLocation.getPath());
                return source;
            } catch (FileNotFoundException ex) {
                logger.error("Cannot locate file " + configLocation.getPath(), ex);
            }
        }
        try {
            InputSource source = new InputSource(configLocation.toURL().openStream());
            source.setSystemId(configLocation.getPath());
            return source;
        } catch (MalformedURLException ex) {
            logger.error("Invalid URL " + configLocation.toString(), ex);
        } catch (IOException ex) {
            logger.error("Unable to access " + configLocation.toString(), ex);
        }
        return null;
    }

    protected InputSource getInputFromString(ClassLoader loader, String configFile) {
        InputSource source;
        try {
            URL url = new URL(configFile);
            source = new InputSource(url.openStream());
            source.setSystemId(configFile);
            return source;
        } catch (Exception ex) {
            source = getInputFromResource(configFile, loader);
            if (source == null) {
                try {
                    InputStream is = new FileInputStream(configFile);
                    source = new InputSource(is);
                    source.setSystemId(configFile);
                } catch (FileNotFoundException fnfe) {
                    // Ignore the exception
                }
            }
        }
        return source;
    }

    protected InputSource getInputFromResource(String resource, ClassLoader loader) {
        InputStream is = Loader.getResourceAsStream(resource, loader);
        if (is == null) {
            return null;
        }
        InputSource source = new InputSource(is);
        source.setSystemId(resource);
        return source;
    }

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

            if (configLocation == null) {
                String config = System.getProperty(CONFIGURATION_FILE_PROPERTY);
                if (config != null) {
                    ClassLoader loader = this.getClass().getClassLoader();
                    InputSource source = getInputFromString(loader, config);
                    if (source != null) {
                        for (ConfigurationFactory factory : factories) {
                            String[] types = factory.getSupportedTypes();
                            if (types != null) {
                                for (String type : types) {
                                    if (type.equals("*") || config.endsWith(type)) {
                                        Configuration c = factory.getConfiguration(source);
                                        if (c != null) {
                                            return c;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                for (ConfigurationFactory factory : factories) {
                    Configuration config = factory.getConfiguration(name, configLocation);
                    if (config != null) {
                        return config;
                    }
                }
            }

            Configuration config = getConfiguration(true, name);
            if (config == null) {
                config = getConfiguration(true, null);
                if (config == null) {
                    config = getConfiguration(false, name);
                    if (config == null) {
                        config = getConfiguration(false, null);
                    }
                }
            }
            return config != null ? config : new DefaultConfiguration();
        }

        private Configuration getConfiguration(boolean isTest, String name) {
            boolean named = (name != null && name.length() > 0);
            ClassLoader loader = this.getClass().getClassLoader();
            for (ConfigurationFactory factory : factories) {
                String configName;
                String prefix = isTest ? TEST_PREFIX : DEFAULT_PREFIX;
                String [] types = factory.getSupportedTypes();
                if (types == null) {
                    continue;
                }

                for (String suffix : types) {
                    if (suffix.equals("*")) {
                        continue;
                    }
                    configName = named ? prefix + name + suffix : prefix + suffix;

                    InputSource source = getInputFromResource(configName, loader);
                    if (source != null) {
                        return factory.getConfiguration(source);
                    }
                }
            }
            return null;
        }

        public String[] getSupportedTypes() {
            return null;
        }

        public Configuration getConfiguration(InputSource source) {
            return null;
        }
    }
}
