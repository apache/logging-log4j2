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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginManager;
import org.apache.logging.log4j.core.config.plugins.PluginType;
import org.apache.logging.log4j.internal.StatusLogger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public class BaseConfiguration implements Configuration {

    private String name;

    private ConcurrentMap<String, Appender> appenders = new ConcurrentHashMap<String, Appender>();

    private ConcurrentMap<String, LoggerConfig> loggers = new ConcurrentHashMap<String, LoggerConfig>();

    private LoggerConfig root = new LoggerConfig();

    private List<Filter> filters = new CopyOnWriteArrayList<Filter>();

    private boolean hasFilters = false;

    private boolean started = false;

    protected Node rootNode;

    protected PluginManager pluginManager;

    protected final static Logger logger = StatusLogger.getLogger();

    protected BaseConfiguration() {
        pluginManager = new PluginManager("Core");
        rootNode = new Node(this);
    }

    public void start() {
        pluginManager.collectPlugins();
        setup();
        doConfigure();
        for (Appender appender : appenders.values()) {
            appender.start();
        }

        for (Filter filter : filters) {
            filter.start();
        }
    }

    public void stop() {
        for (LoggerConfig logger : loggers.values()) {
            logger.clearAppenders();
        }
        for (Appender appender : appenders.values()) {
            appender.stop();
        }
        for (Filter filter : filters) {
            filter.stop();
        }
    }

    protected void setup() {        
    }

    protected void doConfigure() {
        createConfiguration(rootNode);
        for (Node child : rootNode.getChildren()) {
            if (child.getObject() == null) {
                continue;
            }
            if (child.getName().equals("appenders")) {
                appenders = (ConcurrentMap<String, Appender>) child.getObject();
            } else if (child.getName().equals("filters")) {
                filters = new CopyOnWriteArrayList((Filter[]) child.getObject());
            } else if (child.getName().equals("loggers")) {
                Loggers l = (Loggers) child.getObject();
                loggers = l.map;
                root = l.root;
            }
        }

        for (Map.Entry<String, LoggerConfig> entry : loggers.entrySet()) {
            LoggerConfig l = entry.getValue();
            for (String ref : l.getAppenderRefs()) {
                Appender app = appenders.get(ref);
                if (app != null) {
                    l.addAppender(app);
                } else {
                    logger.error("Unable to locate appender " + ref + " for logger " + l.getName());
                }
            }
        }

        setParents();
    }

    protected PluginManager getPluginManager() {
        return pluginManager;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Appender getAppender(String name) {
        return appenders.get(name);
    }

    public Map<String, Appender> getAppenders() {
        return appenders;
    }

    public void addAppender(Appender appender) {
        appenders.put(appender.getName(), appender);
    }

    public void addLoggerAppender(String name, Appender appender) {
        LoggerConfig lc = getLoggerConfig(name);
        if (lc.getName().equals(name)) {
            lc.addAppender(appender);
        } else {
            LoggerConfig nlc = new LoggerConfig(name, lc.getLevel(), lc.isAdditive());
            nlc.addAppender(appender);
            nlc.setParent(lc);
            loggers.putIfAbsent(name, nlc);
            setParents();
        }
    }

    public void removeAppender(String name) {
        for (LoggerConfig logger : loggers.values()) {
            logger.removeAppender(name);
        }
        Appender app = appenders.remove(name);

        if (app != null) {
            app.stop();
        }
    }

    public LoggerConfig getLoggerConfig(String name) {
        if (loggers.containsKey(name)) {
            return loggers.get(name);
        }
        int i = 0;
        String substr = name;
        while ((i = substr.lastIndexOf(".")) > 0) {
            substr = name.substring(0, i);
            if (loggers.containsKey(substr)) {
                return loggers.get(substr);
            }
        }
        return root;
    }

    public LoggerConfig getRootLogger() {
        return root;
    }

    public Map<String, LoggerConfig> getLoggers() {
        return Collections.unmodifiableMap(loggers);
    }

    public LoggerConfig getLogger(String name) {
        return loggers.get(name);
    }

    /**
     * Adding a logger cannot be done atomically so is not allowed in an active configuration. Adding
     * or removing a Logger requires creating a new configuration and then switching.
     *
     * @param name The name of the Logger.
     * @param loggerConfig The LoggerConfig.
     */
    public void addLogger(String name, LoggerConfig loggerConfig) {
        if (started) {
            String msg = "Cannot add logger " + name + " to an active configuration";
            logger.warn(msg);
            throw new IllegalStateException(msg);
        }
        loggers.put(name, loggerConfig);
        setParents();
    }

    /**
     * Removing a logger cannot be done atomically so is not allowed in an active configuration. Adding
     * or removing a Logger requires creating a new configuration and then switching.
     *
     * @param name The name of the Logger.
     */
    public void removeLogger(String name) {
        if (started) {
            String msg = "Cannot remove logger " + name + " in an active configuration";
            logger.warn(msg);
            throw new IllegalStateException(msg);
        }
        loggers.remove(name);
    }

    public Iterator<Filter> getFilters() {
        return filters.iterator();
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
        hasFilters = filters.size() > 0;
    }

    public void removeFilter(Filter filter) {
        filters.remove(filter);
        hasFilters = filters.size() > 0;
    }

    public boolean hasFilters() {
        return hasFilters;
    }

    private void createConfiguration(Node node) {
        for (Node child : node.getChildren()) {
            createConfiguration(child);
        }
        PluginType type = pluginManager.getPluginType(node.getName());
        if (type == null) {
            if (node.getParent() != null) {
                logger.error("Unable to locate plugin for " + node.getName());
            }
        } else {
            node.setObject(createPlugin(type, node));
        }
    }
   /*
    * Retrieve a static public 'method taking a Node parameter on a class specified by its name.
    * @param classClass the class.
    * @return the instantiate method or null if there is none by that
    * description.
    */
    public static Object createPlugin(PluginType type, Node node)
    {
        Class clazz = type.getPluginClass();

        if (Map.class.isAssignableFrom(clazz)) {
            try {
                Map map = (Map) clazz.newInstance();
                for (Node child : node.getChildren()) {
                    map.put(child.getName(), child.getObject());
                }
                return map;
            } catch (Exception ex) {

            }
        }

        if (List.class.isAssignableFrom(clazz)) {
            try {
                List list = (List) clazz.newInstance();
                for (Node child : node.getChildren()) {
                    list.add(child.getObject());
                }
                return list;
            } catch (Exception ex) {

            }
        }

        Method factoryMethod = null;

        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(PluginFactory.class)) {
                factoryMethod = method;
            }
        }
        if (factoryMethod == null) {
            return null;
        }

        try
        {
            int mod = factoryMethod.getModifiers();
            if (!Modifier.isStatic(mod))
            {
                logger.error(factoryMethod.getName() + " method is not static on class " +
                    clazz.getName() + " for element " + node.getName());
                return null;
            }
            logger.debug("Calling " + factoryMethod.getName() + " on class " + clazz.getName() + " for element " +
                node.getName());
            return factoryMethod.invoke(null, node);
        }
        catch (Exception e)
        {
            logger.error("Unable to invoke method " + factoryMethod.getName() + " in class " +
                clazz.getName() + " for element " + node.getName(), e);
        }
        return null;
    }

    private void setParents() {
         for (Map.Entry<String, LoggerConfig> entry : loggers.entrySet()) {
            LoggerConfig logger = entry.getValue();
            String name = entry.getKey();
            if (!name.equals("")) {
                int i = name.lastIndexOf(".");
                if (i > 0) {
                    name = name.substring(0, i);
                    LoggerConfig parent = getLoggerConfig(name);
                    if (parent == null) {
                        parent = root;
                    }
                    logger.setParent(parent);
                }
            }
        }
    }

    @Plugin(name="appenders", type="Core")
    public static class AppendersPlugin {

        @PluginFactory
        public static ConcurrentMap<String, Appender> createAppenders(Node node) {
            ConcurrentMap<String, Appender> map = new ConcurrentHashMap<String, Appender>();

            for (Node child : node.getChildren()) {
                Object obj = child.getObject();
                if (obj != null && obj instanceof Appender) {
                    Appender appender = (Appender) obj;
                    map.put(appender.getName(), appender);
                }
            }

            return map;
        }
    }


    @Plugin(name="filters", type="Core")
    public static class FiltersPlugin {

        @PluginFactory
        public static Filter[] createFilters(Node node) {
            List<Filter> filters = new ArrayList<Filter>();

            for (Node child : node.getChildren()) {
                Object obj = child.getObject();
                if (obj != null && obj instanceof Filter) {
                    filters.add((Filter) obj);
                }
            }

            return filters.toArray(new Filter[filters.size()]);
        }
    }

    @Plugin(name="loggers", type="Core")
    public static class LoggersPlugin {

        @PluginFactory
        public static Loggers createLoggers(Node node) {
            Loggers loggers = new Loggers();

            for (Node child : node.getChildren()) {
                Object obj = child.getObject();
                if (obj != null && obj instanceof LoggerConfig) {
                    LoggerConfig logger = (LoggerConfig) obj;
                    if (logger != null) {
                        if (child.getName().equals("root")) {
                            loggers.root = logger;
                        }
                        loggers.map.put(logger.getName(), logger);
                    }
                }
            }

            return loggers;
        }
    }

    private static class Loggers {
        ConcurrentMap<String, LoggerConfig> map = new ConcurrentHashMap<String, LoggerConfig>();
        LoggerConfig root = null;
    }
}