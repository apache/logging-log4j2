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
package org.apache.log4j.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Experimental ConfigurationFactory for Log4j 1.2 properties files.
 * <p>
 * Currently supports:
 * </p>
 * <ul>
 * <li>log4j.debug</li>
 * <li>log4j.rootLogger</li>
 * <li>log4j.logger</li>
 * <li>log4j.appender</li>
 * <li>org.apache.log4j.ConsoleAppender</li>
 * <li>org.apache.log4j.PatternLayout</li>
 * <ul>
 * <li>Follow</li>
 * <li>Target</li>
 * <li>layout = org.apache.log4j.PatternLayout</li>
 * <li>layout = org.apache.log4j.SimpleLayout</li>
 * <li>layout = org.apache.log4j.TTCCLayout (partial)</li>
 * <li>layout = org.apache.log4j.HtmlLayout (partial)</li>
 * <li>layout = org.apache.log4j.XmlLayout (partial)</li>
 * <li>layout.ConversionPattern</li>
 * </ul>
 * </ul>
 */
// TODO
// @Plugin(name = "Log4j1ConfigurationFactory", category = ConfigurationFactory.CATEGORY)
//
// Best Value?
// @Order(50)
public class Log4j1ConfigurationFactory extends ConfigurationFactory {

    private Map<String, String> buildClassToPropertyPrefixMap(final Properties properties,
            final String[] sortedAppenderNames) {
        final String prefix = "log4j.appender.";
        final int preLength = prefix.length();
        final Map<String, String> map = new HashMap<>(sortedAppenderNames.length);
        for (final Entry<Object, Object> entry : properties.entrySet()) {
            final Object keyObj = entry.getKey();
            if (keyObj != null) {
                final String key = keyObj.toString();
                if (key.startsWith(prefix)) {
                    if (key.indexOf('.', preLength) < 0) {
                        final String name = key.substring(preLength);
                        if (Arrays.binarySearch(sortedAppenderNames, name) >= 0) {
                            final Object value = entry.getValue();
                            if (value != null) {
                                map.put(name, value.toString());
                            }
                        }
                    }
                }
            }
        }
        return map;
    }

    private void buildConsoleAppender(final Properties properties, final String name,
            final ConfigurationBuilder<BuiltConfiguration> builder) {
        final AppenderComponentBuilder appenderBuilder = builder.newAppender(name, "CONSOLE");
        buildConsoleAppenderTarget(properties, name, builder, appenderBuilder);
        buildAppenderLayout(properties, name, builder, appenderBuilder);
        buildConsoleAppenderFollow(properties, name, builder, appenderBuilder);
        builder.add(appenderBuilder);
    }

    private void buildAppenderLayout(final Properties properties, final String name,
            final ConfigurationBuilder<BuiltConfiguration> builder, final AppenderComponentBuilder appenderBuilder) {
        final String layoutValue = getLog4jAppenderValue(properties, name, "layout", null);
        if (layoutValue != null) {
            final String cpValue = getLog4jAppenderValue(properties, name, "layout.ConversionPattern", null);
            switch (layoutValue) {
            case "org.apache.log4j.PatternLayout": {
                appenderBuilder.add(newPatternLayout(builder, cpValue));
                break;
            }
            case "org.apache.log4j.EnhancedPatternLayout": {
                appenderBuilder.add(newPatternLayout(builder, cpValue));
                break;
            }
            case "org.apache.log4j.SimpleLayout": {
                appenderBuilder.add(newPatternLayout(builder, "%level - %m%n"));
                break;
            }
            case "org.apache.log4j.TTCCLayout": {
                // TODO We do not have a %d for the time since the start of the app?
                appenderBuilder.add(newPatternLayout(builder, "%relative [%threadName] %level %logger - %m%n"));
                break;
            }
            case "org.apache.log4j.HTMLLayout": {
                appenderBuilder.add(builder.newLayout("HtmlLayout"));
                break;
            }
            case "org.apache.log4j.XMLLayout": {
                appenderBuilder.add(builder.newLayout("XmlLayout"));
                break;
            }
            default:
                reportWarning("Unsupported value for console appender layout: " + layoutValue);
            }
        }
    }

    private LayoutComponentBuilder newPatternLayout(final ConfigurationBuilder<BuiltConfiguration> builder,
            final String pattern) {
        final LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout");
        if (pattern != null) {
            layoutBuilder.addAttribute("pattern", pattern);
        }
        return layoutBuilder;
    }

    private void buildConsoleAppenderTarget(final Properties properties, final String name,
            final ConfigurationBuilder<BuiltConfiguration> builder, final AppenderComponentBuilder appenderBuilder) {
        final String value = getLog4jAppenderValue(properties, name, "Target", "System.out");
        if (value != null) {
            final Target target;
            switch (value) {
            case "System.out":
                target = ConsoleAppender.Target.SYSTEM_OUT;
                break;
            case "System.err":
                target = ConsoleAppender.Target.SYSTEM_ERR;
                break;
            default:
                reportWarning("Unknow value for console Target: " + value);
                target = null;
            }
            if (target != null) {
                appenderBuilder.addAttribute("target", target);
            }
        }
    }

    private void buildConsoleAppenderFollow(final Properties properties, final String name,
            final ConfigurationBuilder<BuiltConfiguration> builder, final AppenderComponentBuilder appenderBuilder) {
        final String value = getLog4jAppenderValue(properties, name, "Follow", "false");
        if (value != null) {
            appenderBuilder.addAttribute("follow", Boolean.valueOf(value).booleanValue());
        }
    }

    Configuration createConfiguration(final String configName, final URI configLocation,
            final ConfigurationBuilder<BuiltConfiguration> builder) throws IOException {
        builder.setConfigurationName(configName);
        final Properties properties = load(configLocation);
        if (properties == null) {
            return null;
        }
        // DEBUG
        final String debugValue = getLog4jValue(properties, "debug");
        if (Boolean.valueOf(debugValue)) {
            builder.setStatusLevel(Level.DEBUG);
        }
        // Root
        final String[] sortedAppenderNamesC = buildRootLogger(builder, getRootCategoryValue(properties));
        final String[] sortedAppenderNamesL = buildRootLogger(builder, getRootLoggerValue(properties));
        final String[] sortedAppenderNames = sortedAppenderNamesL.length > 0 ? sortedAppenderNamesL
                : sortedAppenderNamesC;
        // Appenders
        final Map<String, String> classNameToProperty = buildClassToPropertyPrefixMap(properties, sortedAppenderNames);
        for (final Entry<String, String> entry : classNameToProperty.entrySet()) {
            final String appenderName = entry.getKey();
            switch (entry.getValue()) {
            case "org.apache.log4j.ConsoleAppender":
                buildConsoleAppender(properties, appenderName, builder);
                break;
            default:
                reportWarning("Ignoring appender " + appenderName
                        + "; consider porting your configuration file to the current Log4j format.");
            }
        }
        // Loggers
        buildLoggers(properties, "log4j.category.", builder);
        buildLoggers(properties, "log4j.logger.", builder);
        return builder.build();
    }

    private String[] buildRootLogger(final ConfigurationBuilder<BuiltConfiguration> builder,
            final String rootLoggerValue) {
        if (rootLoggerValue == null) {
            return new String[0];
        }
        final String[] rootLoggerParts = rootLoggerValue.split("\\s*,\\s*");
        final Level rootLoggerLevel = rootLoggerParts.length > 0 ? Level.valueOf(rootLoggerParts[0]) : Level.ERROR;
        builder.add(builder.newRootLogger(rootLoggerLevel));
        final String[] sortedAppenderNames = Arrays.copyOfRange(rootLoggerParts, 1, rootLoggerParts.length);
        Arrays.sort(sortedAppenderNames);
        return sortedAppenderNames;
    }

    private void buildLoggers(final Properties properties, final String prefix,
            final ConfigurationBuilder<BuiltConfiguration> builder) {
        final int preLength = prefix.length();
        for (final Entry<Object, Object> entry : properties.entrySet()) {
            final Object keyObj = entry.getKey();
            if (keyObj != null) {
                final String key = keyObj.toString();
                if (key.startsWith(prefix)) {
                    final String name = key.substring(preLength);
                    final Object value = entry.getValue();
                    if (value != null) {
                        builder.add(builder.newLogger(name, Level.valueOf(value.toString())));
                    }
                }
            }
        }

    }

    @Override
    public Configuration getConfiguration(final ConfigurationSource source) {
        return getConfiguration(source.toString(), null);
    }

    @Override
    public Configuration getConfiguration(final String name, final URI configLocation) {
        try {
            return createConfiguration(name, configLocation, newConfigurationBuilder());
        } catch (final IOException e) {
            StatusLogger.getLogger().error(e);
            return null;
        }
    }

    private String getLog4jAppenderValue(final Properties properties, final String appenderName,
            final String attributeName, final String defaultValue) {
        return properties.getProperty("log4j.appender." + appenderName + "." + attributeName, defaultValue);
    }

    private String getLog4jValue(final Properties properties, final String key) {
        return properties.getProperty("log4j." + key);
    }

    private String getRootCategoryValue(final Properties properties) {
        return getLog4jValue(properties, "rootCategory");
    }

    private String getRootLoggerValue(final Properties properties) {
        return getLog4jValue(properties, "rootLogger");
    }

    @Override
    protected String[] getSupportedTypes() {
        return new String[] { "*.properties", ".xml" };
    }

    private Properties load(final URI uri) throws IOException {
        final Properties properties = toProperties(uri);
        final String rootCategoryValue = getRootCategoryValue(properties);
        final String rootLoggerValue = getRootLoggerValue(properties);
        if (rootCategoryValue == null && rootLoggerValue == null) {
            // This is not a Log4j 1 properties file.
            return null;
        }
        return properties;
    }

    private void reportWarning(final String msg) {
        StatusLogger.getLogger().warn("Log4j version 1 to 2 configuration bridge: " + msg);

    }

    private Properties toProperties(final URI uri) throws IOException {
        final Properties properties;
        try (InputStream in = uri.toURL().openStream()) {
            properties = new Properties();
            if (uri.toString().endsWith(".xml")) {
                properties.loadFromXML(in);
            } else {
                properties.load(in);
            }
        }
        return properties;
    }
}
