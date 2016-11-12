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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * Experimental parser for Log4j 1.2 properties configuration files.
 *
 * This class is not thread-safe.
 * 
 * <p>
 * From the Log4j 1.2 Javadocs:
 * </p>
 * <p>
 * All option values admit variable substitution. The syntax of variable substitution is similar to that of Unix shells. The string between
 * an opening "${" and closing "}" is interpreted as a key. The value of the substituted variable can be defined as a system property or in
 * the configuration file itself. The value of the key is first searched in the system properties, and if not found there, it is then
 * searched in the configuration file being parsed. The corresponding value replaces the ${variableName} sequence. For example, if java.home
 * system property is set to /home/xyz, then every occurrence of the sequence ${java.home} will be interpreted as /home/xyz.
 * </p>
 */
public class Log4j1ConfigurationParser {

    private static final String COMMA_DELIMITED_RE = "\\s*,\\s*";
    private static final String ROOTLOGGER = "rootLogger";
    private static final String ROOTCATEGORY = "rootCategory";
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    private final Properties properties = new Properties();
    private StrSubstitutor strSubstitutorProperties;
    private StrSubstitutor strSubstitutorSystem;

    private final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory
            .newConfigurationBuilder();

    /**
     * Parses a Log4j 1.2 properties configuration file in ISO 8859-1 encoding into a ConfigurationBuilder.
     *
     * @param input
     *            InputStream to read from is assumed to be ISO 8859-1, and will not be closed.
     * @return the populated ConfigurationBuilder, never {@literal null}
     * @throws IOException
     *             if unable to read the input
     * @throws ConfigurationException
     *             if the input does not contain a valid configuration
     */
    public ConfigurationBuilder<BuiltConfiguration> buildConfigurationBuilder(final InputStream input)
            throws IOException {
        try {
            properties.load(input);
            strSubstitutorProperties = new StrSubstitutor(properties);
            strSubstitutorSystem = new StrSubstitutor(System.getProperties());
            final String rootCategoryValue = getLog4jValue(ROOTCATEGORY);
            final String rootLoggerValue = getLog4jValue(ROOTLOGGER);
            if (rootCategoryValue == null && rootLoggerValue == null) {
                // This is not a Log4j 1 properties configuration file.
                warn("Missing " + ROOTCATEGORY + " or " + ROOTLOGGER + " in " + input);
                // throw new ConfigurationException(
                // "Missing " + ROOTCATEGORY + " or " + ROOTLOGGER + " in " + input);
            }
            builder.setConfigurationName("Log4j1");
            // DEBUG
            final String debugValue = getLog4jValue("debug");
            if (Boolean.valueOf(debugValue)) {
                builder.setStatusLevel(Level.DEBUG);
            }
            // Root
            buildRootLogger(getLog4jValue(ROOTCATEGORY));
            buildRootLogger(getLog4jValue(ROOTLOGGER));
            // Appenders
            final Map<String, String> appenderNameToClassName = buildClassToPropertyPrefixMap();
            for (final Map.Entry<String, String> entry : appenderNameToClassName.entrySet()) {
                final String appenderName = entry.getKey();
                final String appenderClass = entry.getValue();
                buildAppender(appenderName, appenderClass);
            }
            // Loggers
            buildLoggers("log4j.category.");
            buildLoggers("log4j.logger.");
            buildProperties();
            return builder;
        } catch (final IllegalArgumentException e) {
            throw new ConfigurationException(e);
        }
    }

    private void buildProperties() {
        for (final Map.Entry<Object, Object> entry : new TreeMap<>(properties).entrySet()) {
            final String key = entry.getKey().toString();
            if (!key.startsWith("log4j.") && !key.equals(ROOTCATEGORY) && !key.equals(ROOTLOGGER)) {
                builder.addProperty(key, Objects.toString(entry.getValue(), Strings.EMPTY));
            }
        }
    }

    private void warn(final String string) {
        System.err.println(string);
    }

    private Map<String, String> buildClassToPropertyPrefixMap() {
        final String prefix = "log4j.appender.";
        final int preLength = prefix.length();
        final Map<String, String> map = new HashMap<>();
        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            final Object keyObj = entry.getKey();
            if (keyObj != null) {
                final String key = keyObj.toString();
                if (key.startsWith(prefix)) {
                    if (key.indexOf('.', preLength) < 0) {
                        final String name = key.substring(preLength);
                        final Object value = entry.getValue();
                        if (value != null) {
                            map.put(name, value.toString());
                        }
                    }
                }
            }
        }
        return map;
    }

    private void buildAppender(final String appenderName, final String appenderClass) {
        switch (appenderClass) {
        case "org.apache.log4j.ConsoleAppender":
            buildConsoleAppender(appenderName);
            break;
        case "org.apache.log4j.FileAppender":
            buildFileAppender(appenderName);
            break;
        case "org.apache.log4j.DailyRollingFileAppender":
            buildDailyRollingFileAppender(appenderName);
            break;
        case "org.apache.log4j.RollingFileAppender":
            buildRollingFileAppender(appenderName);
            break;
        case "org.apache.log4j.varia.NullAppender":
            buildNullAppender(appenderName);
            break;
        default:
            reportWarning("Unknown appender class: " + appenderClass + "; ignoring appender: " + appenderName);
        }
    }

    private void buildConsoleAppender(final String appenderName) {
        final AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName, ConsoleAppender.PLUGIN_NAME);
        final String targetValue = getLog4jAppenderValue(appenderName, "Target", "System.out");
        if (targetValue != null) {
            final ConsoleAppender.Target target;
            switch (targetValue) {
            case "System.out":
                target = ConsoleAppender.Target.SYSTEM_OUT;
                break;
            case "System.err":
                target = ConsoleAppender.Target.SYSTEM_ERR;
                break;
            default:
                reportWarning("Unknown value for console Target: " + targetValue);
                target = null;
            }
            if (target != null) {
                appenderBuilder.addAttribute("target", target);
            }
        }
        buildAttribute(appenderName, appenderBuilder, "Follow", "follow");
        if (FALSE.equalsIgnoreCase(getLog4jAppenderValue(appenderName, "ImmediateFlush"))) {
            reportWarning("ImmediateFlush=false is not supported on Console appender");
        }
        buildAppenderLayout(appenderName, appenderBuilder);
        builder.add(appenderBuilder);
    }

    private void buildFileAppender(final String appenderName) {
        final AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName, FileAppender.PLUGIN_NAME);
        buildFileAppender(appenderName, appenderBuilder);
        builder.add(appenderBuilder);
    }

    private void buildFileAppender(final String appenderName, final AppenderComponentBuilder appenderBuilder) {
        buildMandatoryAttribute(appenderName, appenderBuilder, "File", "fileName");
        buildAttribute(appenderName, appenderBuilder, "Append", "append");
        buildAttribute(appenderName, appenderBuilder, "BufferedIO", "bufferedIo");
        buildAttribute(appenderName, appenderBuilder, "BufferSize", "bufferSize");
        buildAttribute(appenderName, appenderBuilder, "ImmediateFlush", "immediateFlush");
        buildAppenderLayout(appenderName, appenderBuilder);
    }

    private void buildDailyRollingFileAppender(final String appenderName) {
        final AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName,
                RollingFileAppender.PLUGIN_NAME);
        buildFileAppender(appenderName, appenderBuilder);
        final String fileName = getLog4jAppenderValue(appenderName, "File");
        final String datePattern = getLog4jAppenderValue(appenderName, "DatePattern", fileName + "'.'yyyy-MM-dd");
        appenderBuilder.addAttribute("filePattern", fileName + "%d{" + datePattern + "}");
        final ComponentBuilder<?> triggeringPolicy = builder.newComponent("Policies")
                .addComponent(builder.newComponent("TimeBasedTriggeringPolicy").addAttribute("modulate", true));
        appenderBuilder.addComponent(triggeringPolicy);
        appenderBuilder
                .addComponent(builder.newComponent("DefaultRolloverStrategy").addAttribute("max", Integer.MAX_VALUE));
        builder.add(appenderBuilder);
    }

    private void buildRollingFileAppender(final String appenderName) {
        final AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName,
                RollingFileAppender.PLUGIN_NAME);
        buildFileAppender(appenderName, appenderBuilder);
        final String fileName = getLog4jAppenderValue(appenderName, "File");
        appenderBuilder.addAttribute("filePattern", fileName + ".%i");
        final String maxFileSizeString = getLog4jAppenderValue(appenderName, "MaxFileSize", "10485760");
        final String maxBackupIndexString = getLog4jAppenderValue(appenderName, "MaxBackupIndex", "1");
        final ComponentBuilder<?> triggeringPolicy = builder.newComponent("Policies").addComponent(
                builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", maxFileSizeString));
        appenderBuilder.addComponent(triggeringPolicy);
        appenderBuilder.addComponent(
                builder.newComponent("DefaultRolloverStrategy").addAttribute("max", maxBackupIndexString));
        builder.add(appenderBuilder);
    }

    private void buildAttribute(final String componentName, final ComponentBuilder componentBuilder,
            final String sourceAttributeName, final String targetAttributeName) {
        final String attributeValue = getLog4jAppenderValue(componentName, sourceAttributeName);
        if (attributeValue != null) {
            componentBuilder.addAttribute(targetAttributeName, attributeValue);
        }
    }

    private void buildAttributeWithDefault(final String componentName, final ComponentBuilder componentBuilder,
            final String sourceAttributeName, final String targetAttributeName, final String defaultValue) {
        final String attributeValue = getLog4jAppenderValue(componentName, sourceAttributeName, defaultValue);
        componentBuilder.addAttribute(targetAttributeName, attributeValue);
    }

    private void buildMandatoryAttribute(final String componentName, final ComponentBuilder componentBuilder,
            final String sourceAttributeName, final String targetAttributeName) {
        final String attributeValue = getLog4jAppenderValue(componentName, sourceAttributeName);
        if (attributeValue != null) {
            componentBuilder.addAttribute(targetAttributeName, attributeValue);
        } else {
            reportWarning("Missing " + sourceAttributeName + " for " + componentName);
        }
    }

    private void buildNullAppender(final String appenderName) {
        final AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName, NullAppender.PLUGIN_NAME);
        builder.add(appenderBuilder);
    }

    private void buildAppenderLayout(final String name, final AppenderComponentBuilder appenderBuilder) {
        final String layoutClass = getLog4jAppenderValue(name, "layout", null);
        if (layoutClass != null) {
            switch (layoutClass) {
            case "org.apache.log4j.PatternLayout":
            case "org.apache.log4j.EnhancedPatternLayout": {
                final String pattern = getLog4jAppenderValue(name, "layout.ConversionPattern", null)

                        // Log4j 2's %x (NDC) is not compatible with Log4j 1's
                        // %x
                        // Log4j 1: "foo bar baz"
                        // Log4j 2: "[foo, bar, baz]"
                        // Use %ndc to get the Log4j 1 format
                        .replace("%x", "%ndc")

                        // Log4j 2's %X (MDC) is not compatible with Log4j 1's
                        // %X
                        // Log4j 1: "{{foo,bar}{hoo,boo}}"
                        // Log4j 2: "{foo=bar,hoo=boo}"
                        // Use %properties to get the Log4j 1 format
                        .replace("%X", "%properties");

                appenderBuilder.add(newPatternLayout(pattern));
                break;
            }
            case "org.apache.log4j.SimpleLayout": {
                appenderBuilder.add(newPatternLayout("%level - %m%n"));
                break;
            }
            case "org.apache.log4j.TTCCLayout": {
                String pattern = "%r ";
                if (Boolean.parseBoolean(getLog4jAppenderValue(name, "layout.ThreadPrinting", TRUE))) {
                    pattern += "[%t] ";
                }
                pattern += "%p ";
                if (Boolean.parseBoolean(getLog4jAppenderValue(name, "layout.CategoryPrefixing", TRUE))) {
                    pattern += "%c ";
                }
                if (Boolean.parseBoolean(getLog4jAppenderValue(name, "layout.ContextPrinting", TRUE))) {
                    pattern += "%notEmpty{%ndc }";
                }
                pattern += "- %m%n";
                appenderBuilder.add(newPatternLayout(pattern));
                break;
            }
            case "org.apache.log4j.HTMLLayout": {
                final LayoutComponentBuilder htmlLayout = builder.newLayout("HtmlLayout");
                htmlLayout.addAttribute("title", getLog4jAppenderValue(name, "layout.Title", "Log4J Log Messages"));
                htmlLayout.addAttribute("locationInfo",
                        Boolean.parseBoolean(getLog4jAppenderValue(name, "layout.LocationInfo", FALSE)));
                appenderBuilder.add(htmlLayout);
                break;
            }
            case "org.apache.log4j.xml.XMLLayout": {
                final LayoutComponentBuilder xmlLayout = builder.newLayout("Log4j1XmlLayout");
                xmlLayout.addAttribute("locationInfo",
                        Boolean.parseBoolean(getLog4jAppenderValue(name, "layout.LocationInfo", FALSE)));
                xmlLayout.addAttribute("properties",
                        Boolean.parseBoolean(getLog4jAppenderValue(name, "layout.Properties", FALSE)));
                appenderBuilder.add(xmlLayout);
                break;
            }
            default:
                reportWarning("Unknown layout class: " + layoutClass);
            }
        }
    }

    private LayoutComponentBuilder newPatternLayout(final String pattern) {
        final LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout");
        if (pattern != null) {
            layoutBuilder.addAttribute("pattern", pattern);
        }
        return layoutBuilder;
    }

    private void buildRootLogger(final String rootLoggerValue) {
        if (rootLoggerValue == null) {
            return;
        }
        final String[] rootLoggerParts = rootLoggerValue.split(COMMA_DELIMITED_RE);
        final String rootLoggerLevel = getLevelString(rootLoggerParts, Level.ERROR.name());
        final RootLoggerComponentBuilder loggerBuilder = builder.newRootLogger(rootLoggerLevel);
        //
        final String[] sortedAppenderNames = Arrays.copyOfRange(rootLoggerParts, 1, rootLoggerParts.length);
        Arrays.sort(sortedAppenderNames);
        for (final String appender : sortedAppenderNames) {
            loggerBuilder.add(builder.newAppenderRef(appender));
        }
        builder.add(loggerBuilder);
    }

    private String getLevelString(final String[] loggerParts, final String defaultLevel) {
        return loggerParts.length > 0 ? loggerParts[0] : defaultLevel;
    }

    private void buildLoggers(final String prefix) {
        final int preLength = prefix.length();
        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            final Object keyObj = entry.getKey();
            if (keyObj != null) {
                final String key = keyObj.toString();
                if (key.startsWith(prefix)) {
                    final String name = key.substring(preLength);
                    final Object value = entry.getValue();
                    if (value != null) {
                        // a Level may be followed by a list of Appender refs.
                        final String valueStr = value.toString();
                        final String[] split = valueStr.split(COMMA_DELIMITED_RE);
                        final String level = getLevelString(split, null);
                        if (level == null) {
                            warn("Level is missing for entry " + entry);
                        } else {
                            final LoggerComponentBuilder newLogger = builder.newLogger(name, level);
                            if (split.length > 1) {
                                // Add Appenders to this logger
                                final String[] sortedAppenderNames = Arrays.copyOfRange(split, 1, split.length);
                                Arrays.sort(sortedAppenderNames);
                                for (final String appenderName : sortedAppenderNames) {
                                    newLogger.add(builder.newAppenderRef(appenderName));
                                }
                            }
                            builder.add(newLogger);
                        }
                    }
                }
            }
        }
    }

    private String getLog4jAppenderValue(final String appenderName, final String attributeName) {
        return getProperty("log4j.appender." + appenderName + "." + attributeName);
    }

    private String getProperty(final String key) {
        final String value = properties.getProperty(key);
        final String sysValue = strSubstitutorSystem.replace(value);
        return strSubstitutorProperties.replace(sysValue);
    }

    private String getProperty(final String key, final String defaultValue) {
        final String value = getProperty(key);
        return value == null ? defaultValue : value;
    }

    private String getLog4jAppenderValue(final String appenderName, final String attributeName,
            final String defaultValue) {
        return getProperty("log4j.appender." + appenderName + "." + attributeName, defaultValue);
    }

    private String getLog4jValue(final String key) {
        return getProperty("log4j." + key);
    }

    private void reportWarning(final String msg) {
        StatusLogger.getLogger().warn("Log4j 1 configuration parser: " + msg);
    }

}
