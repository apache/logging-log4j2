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
package org.apache.logging.log4j.core.config;

import java.util.Map;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.net.Advertiser;

/**
 * Interface that must be implemented to create a configuration.
 */
public interface Configuration extends Filterable {

    /** Key for storing the Context properties. */
    String CONTEXT_PROPERTIES = "ContextProperties";

    /**
     * Returns the configuration name.
     * @return the name of the configuration.
     */
    String getName();

    /**
     * Locates the appropriate LoggerConfig for a Logger name. This will remove tokens from the
     * package name as necessary or return the root LoggerConfig if no other matches were found.
     * @param name The Logger name.
     * @return The located LoggerConfig.
     */
    LoggerConfig getLoggerConfig(String name);

    /**
     * Returns a Map containing all the Appenders and their name.
     * @return A Map containing each Appender's name and the Appender object.
     */
    Map<String, Appender> getAppenders();

    Map<String, LoggerConfig> getLoggers();

    void addLoggerAppender(Logger logger, Appender appender);

    void addLoggerFilter(Logger logger, Filter filter);

    void setLoggerAdditive(Logger logger, boolean additive);

    Map<String, String> getProperties();

    void start();

    void stop();

    void addListener(ConfigurationListener listener);

    void removeListener(ConfigurationListener listener);

    StrSubstitutor getStrSubstitutor();

    void createConfiguration(Node node, LogEvent event);

    <T> T getComponent(String name);

    void addComponent(String name, Object object);

    void setConfigurationMonitor(ConfigurationMonitor monitor);

    ConfigurationMonitor getConfigurationMonitor();

    void setAdvertiser(Advertiser advertiser);

    Advertiser getAdvertiser();

    boolean isShutdownHookEnabled();
}
