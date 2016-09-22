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
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Experimental parser for Log4j 1.2 properties configuration files.
 *
 * This class is not thread-safe.
 */
public class Log4j1ConfigurationParser {

	private final Properties properties = new Properties();
	private StrSubstitutor strSubstitutor;
	
	private final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory
			.newConfigurationBuilder();

	/**
	 * Parse a Log4j 1.2 properties configuration file into a
	 * ConfigurationBuilder.
	 *
	 * @param input
	 *            InputStream to read from, will not be closed.
	 * @return the populated ConfigurationBuilder, never {@literal null}
	 * @throws IOException
	 *             if unable to read the input
	 * @throws ConfigurationException
	 *             if the input does not contain a valid configuration
	 */
	public ConfigurationBuilder<BuiltConfiguration> buildConfigurationBuilder(final InputStream input)
			throws IOException {
		properties.load(input);
		strSubstitutor = new StrSubstitutor(properties);
		final String rootCategoryValue = getLog4jValue("rootCategory");
		final String rootLoggerValue = getLog4jValue("rootLogger");
		if (rootCategoryValue == null && rootLoggerValue == null) {
			// This is not a Log4j 1 properties configuration file.
			throw new ConfigurationException("Input does not contain a valid Log4j 1.x properties configuration");
		}
		builder.setConfigurationName("Log4j1");
		// DEBUG
		final String debugValue = getLog4jValue("debug");
		if (Boolean.valueOf(debugValue)) {
			builder.setStatusLevel(Level.DEBUG);
		}
		// Root
		final String[] sortedAppenderNamesC = buildRootLogger(getLog4jValue("rootCategory"));
		final String[] sortedAppenderNamesL = buildRootLogger(getLog4jValue("rootLogger"));
		final String[] sortedAppenderNames = sortedAppenderNamesL.length > 0 ? sortedAppenderNamesL
				: sortedAppenderNamesC;
		// Appenders
		final Map<String, String> classNameToProperty = buildClassToPropertyPrefixMap(sortedAppenderNames);
		for (final Map.Entry<String, String> entry : classNameToProperty.entrySet()) {
			final String appenderName = entry.getKey();
			final String appenderClass = entry.getValue();
			buildAppender(appenderName, appenderClass);
		}
		// Loggers
		buildLoggers("log4j.category.");
		buildLoggers("log4j.logger.");
		return builder;
	}

	private Map<String, String> buildClassToPropertyPrefixMap(final String[] sortedAppenderNames) {
		final String prefix = "log4j.appender.";
		final int preLength = prefix.length();
		final Map<String, String> map = new HashMap<>(sortedAppenderNames.length);
		for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
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

	private void buildAppender(final String appenderName, final String appenderClass) {
		switch (appenderClass) {
		case "org.apache.log4j.ConsoleAppender":
			buildConsoleAppender(appenderName);
			break;
		case "org.apache.log4j.FileAppender":
			buildFileAppender(appenderName);
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
		final AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName, "Console");
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
		if ("false".equalsIgnoreCase(getLog4jAppenderValue(appenderName, "ImmediateFlush"))) {
			reportWarning("ImmediateFlush=false is not supported on Console appender");
		}
		buildAppenderLayout(appenderName, appenderBuilder);
		builder.add(appenderBuilder);
	}

	private void buildFileAppender(final String appenderName) {
		final AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName, "File");
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

	private void buildRollingFileAppender(final String appenderName) {
		final AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName, "RollingFile");
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

	private void buildNullAppender(String appenderName) {
		final AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName, "Null");
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
				if (Boolean.parseBoolean(getLog4jAppenderValue(name, "layout.ThreadPrinting", "true"))) {
					pattern += "[%t] ";
				}
				pattern += "%p ";
				if (Boolean.parseBoolean(getLog4jAppenderValue(name, "layout.CategoryPrefixing", "true"))) {
					pattern += "%c ";
				}
				if (Boolean.parseBoolean(getLog4jAppenderValue(name, "layout.ContextPrinting", "true"))) {
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
						Boolean.parseBoolean(getLog4jAppenderValue(name, "layout.LocationInfo", "false")));
				appenderBuilder.add(htmlLayout);
				break;
			}
			case "org.apache.log4j.xml.XMLLayout": {
				final LayoutComponentBuilder xmlLayout = builder.newLayout("Log4j1XmlLayout");
				xmlLayout.addAttribute("locationInfo",
						Boolean.parseBoolean(getLog4jAppenderValue(name, "layout.LocationInfo", "false")));
				xmlLayout.addAttribute("properties",
						Boolean.parseBoolean(getLog4jAppenderValue(name, "layout.Properties", "false")));
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

	private String[] buildRootLogger(final String rootLoggerValue) {
		if (rootLoggerValue == null) {
			return new String[0];
		}
		final String[] rootLoggerParts = rootLoggerValue.split("\\s*,\\s*");
		final Level rootLoggerLevel = rootLoggerParts.length > 0 ? Level.valueOf(rootLoggerParts[0]) : Level.ERROR;
		final String[] sortedAppenderNames = Arrays.copyOfRange(rootLoggerParts, 1, rootLoggerParts.length);
		Arrays.sort(sortedAppenderNames);
		final RootLoggerComponentBuilder loggerBuilder = builder.newRootLogger(rootLoggerLevel);
		for (final String appender : sortedAppenderNames) {
			loggerBuilder.add(builder.newAppenderRef(appender));
		}
		builder.add(loggerBuilder);
		return sortedAppenderNames;
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
						builder.add(builder.newLogger(name, Level.valueOf(value.toString())));
					}
				}
			}
		}
	}

	private String getLog4jAppenderValue(final String appenderName, final String attributeName) {
		return getProperty("log4j.appender." + appenderName + "." + attributeName);
	}

	private String getProperty(final String key) {
		return strSubstitutor.replace(properties.getProperty(key));
	}

	private String getProperty(final String key, String defaultValue) {
		return strSubstitutor.replace(properties.getProperty(key, defaultValue));
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
