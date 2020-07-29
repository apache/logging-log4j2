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
package org.apache.log4j.builders;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.builders.appender.AppenderBuilder;
import org.apache.log4j.builders.filter.FilterBuilder;
import org.apache.log4j.builders.layout.LayoutBuilder;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 *
 */
public class BuilderManager {

    public static final String CATEGORY = "Log4j Builder";
    private static final Logger LOGGER = StatusLogger.getLogger();
    private final Map<String, PluginType<?>> plugins;

    public BuilderManager() {
        final PluginManager manager = new PluginManager(CATEGORY);
        manager.collectPlugins();
        plugins = manager.getPlugins();
    }

    public Appender parseAppender(String className, Element appenderElement, XmlConfigurationFactory factory) {
        PluginType<?> plugin = plugins.get(className.toLowerCase());
        if (plugin != null) {
            try {
                @SuppressWarnings("unchecked")
                AppenderBuilder builder = (AppenderBuilder) LoaderUtil.newInstanceOf(plugin.getPluginClass());
                return builder.parseAppender(appenderElement, factory);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                LOGGER.warn("Unable to load plugin: {} due to: {}", plugin.getKey(), ex.getMessage());
            }
        }
        return null;
    }

    public Filter parseFilter(String className, Element filterElement, XmlConfigurationFactory factory) {
        PluginType<?> plugin = plugins.get(className.toLowerCase());
        if (plugin != null) {
            try {
                @SuppressWarnings("unchecked")
                FilterBuilder builder = (FilterBuilder) LoaderUtil.newInstanceOf(plugin.getPluginClass());
                return builder.parseFilter(filterElement, factory);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                LOGGER.warn("Unable to load plugin: {} due to: {}", plugin.getKey(), ex.getMessage());
            }
        }
        return null;
    }


    public Layout parseLayout(String className, Element layoutElement, XmlConfigurationFactory factory) {
        PluginType<?> plugin = plugins.get(className.toLowerCase());
        if (plugin != null) {
            try {
                @SuppressWarnings("unchecked")
                LayoutBuilder builder = (LayoutBuilder) LoaderUtil.newInstanceOf(plugin.getPluginClass());
                return builder.parseLayout(layoutElement, factory);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                LOGGER.warn("Unable to load plugin: {} due to: {}", plugin.getKey(), ex.getMessage());
            }
        }
        return null;
    }

}
