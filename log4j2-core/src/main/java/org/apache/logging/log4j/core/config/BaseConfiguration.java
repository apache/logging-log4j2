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
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginManager;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginType;
import org.apache.logging.log4j.core.helpers.NameUtil;
import org.apache.logging.log4j.internal.StatusLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
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
        rootNode = new Node();
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
                loggers = l.getMap();
                root = l.getRoot();
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

    public void addLoggerAppender(org.apache.logging.log4j.core.Logger logger, Appender appender) {
        String name = logger.getName();
        LoggerConfig lc = getLoggerConfig(name);
        if (lc.getName().equals(name)) {
            lc.addAppender(appender);
        } else {
            LoggerConfig nlc = new LoggerConfig(name, lc.getLevel(), lc.isAdditive());
            nlc.addAppender(appender);
            nlc.setParent(lc);
            loggers.putIfAbsent(name, nlc);
            setParents();
            logger.getContext().updateLoggers();
        }
    }

    public void addLoggerFilter(org.apache.logging.log4j.core.Logger logger, Filter filter) {
        String name = logger.getName();
        LoggerConfig lc = getLoggerConfig(name);
        if (lc.getName().equals(name)) {
            lc.addFilter(filter);
        } else {
            LoggerConfig nlc = new LoggerConfig(name, lc.getLevel(), lc.isAdditive());
            nlc.addFilter(filter);
            nlc.setParent(lc);
            loggers.putIfAbsent(name, nlc);
            setParents();
            logger.getContext().updateLoggers();
        }
    }

    public void setLoggerAdditive(org.apache.logging.log4j.core.Logger logger, boolean additive) {
        String name = logger.getName();
        LoggerConfig lc = getLoggerConfig(name);
        if (lc.getName().equals(name)) {
            lc.setAdditive(additive);
        } else {
            LoggerConfig nlc = new LoggerConfig(name, lc.getLevel(), additive);
            nlc.setParent(lc);
            loggers.putIfAbsent(name, nlc);
            setParents();
            logger.getContext().updateLoggers();
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
        while ((substr = NameUtil.getSubName(substr)) != null) {
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
        PluginType type = node.getType();
        if (type == null) {
            if (node.getParent() != null) {
                logger.error("Unable to locate plugin for " + node.getName());
            }
        } else {
            node.setObject(createPluginObject(type, node));
        }
    }
   /*
    * Retrieve a static public 'method to create the desired object. Every parameter
    * will be annotated to identify the appropriate attribute or element to use to
    * set the value of the paraemter.
    * Parameters annotated with PluginAttr will always be set as Strings.
    * Parameters annotated with PluginElement may be Objects or arrays. Collections
    * and Maps are currently not supported, although the factory method that is called
    * can create these from an array.
    *
    * Although the happy path works, more work still needs to be done to log incorrect
    * parameters. These will generally result in unhelpful InvocationTargetExceptions.
    * @param classClass the class.
    * @return the instantiate method or null if there is none by that
    * description.
    */
    public static Object createPluginObject(PluginType type, Node node)
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
                break;
            }
        }
        if (factoryMethod == null) {
            return null;
        }

        Annotation[][] parmArray = factoryMethod.getParameterAnnotations();
        Class[] parmClasses = factoryMethod.getParameterTypes();
        if (parmArray.length != parmClasses.length) {
            logger.error("");
        }
        Object[] parms = new Object[parmClasses.length];

        int index = 0;
        Map<String, String> attrs = node.getAttributes();
        List<Node> children = node.getChildren();
        StringBuilder sb = new StringBuilder();
        List<Node> used = new ArrayList<Node>();

        /*
         * For each parameter:
         * If the parameter is an attribute store the value of the attribute in the parameter array.
         * If the parameter is an element:
         *   Determine if the required parameter is an array.
         *     If so, if a child contains the array, use it,
         *      otherwise create the array from all child nodes of the correct type.
         *     Store the array into the parameter array.
         *   If not an array, store the object in the child node into the parameter array.
         */
        for (Annotation[] parmTypes : parmArray) {
            for (Annotation a : parmTypes) {
                if (sb.length() == 0) {
                    sb.append(" with params(");
                } else {
                    sb.append(", ");
                }
                if (a instanceof PluginAttr) {
                    String name = ((PluginAttr)a).value();
                    String value = getAttrValue(name, attrs);
                    sb.append(name +"=" + "\"" + value + "\"");
                    parms[index] = value;
                } else if (a instanceof PluginElement) {
                    PluginElement elem = (PluginElement)a;
                    String name = elem.value();
                    Class parmClass = parmClasses[index].getComponentType();
                    if (parmClasses[index].isArray()) {
                        List<Object> list = new ArrayList<Object>();
                        sb.append("{");
                        boolean first = true;
                        for (Node child : children) {
                            PluginType childType = child.getType();
                            if (elem.value().equals(childType.getElementName()) ||
                                parmClass.isAssignableFrom(childType.getPluginClass())) {
                                used.add(child);
                                if (!first) {
                                    sb.append(", ");
                                }
                                first = false;
                                Object obj = child.getObject();
                                if (obj.getClass().isArray()) {
                                    printArray(sb, (Object[])obj);
                                    parms[index] = obj;
                                    break;
                                }
                                sb.append(child.toString());
                                list.add(obj);
                            }
                        }
                        sb.append("}");
                        if (parms[index] != null) {
                            break;
                        }
                        Object[] array = (Object[]) Array.newInstance(parmClass, list.size());
                        int i=0;
                        for (Object obj : list) {
                            array[i] = obj;
                            ++i;
                        }
                        parms[index] = array;
                    } else {
                        for (Node child : children) {
                            sb.append(child.toString());
                            PluginType childType = child.getType();
                            if (elem.value().equals(childType.getElementName()) ||
                                parmClass.isAssignableFrom(childType.getPluginClass())) {
                                used.add(child);
                                parms[index] = child.getObject();
                                break;
                            }
                        }
                    }
                }
            }
            ++index;
        }
        if (sb.length() > 0) {
            sb.append(")");
        }

        if (attrs.size() > 0) {
            StringBuilder eb = new StringBuilder();
            for (String key : attrs.keySet()) {
                if (eb.length() == 0) {
                    eb.append(node.getName());
                    eb.append(" contains ");
                    if (attrs.size() == 1) {
                        eb.append("an invalid element or attribute ");
                    } else {
                        eb.append("invalid attributes ");
                    }
                } else {
                    eb.append(", ");
                }
                eb.append("\"");
                eb.append(key);
                eb.append("\"");

            }
            logger.error(eb.toString());
        }

        if (used.size() != children.size()) {
            for (Node child : children) {
                if (used.contains(child)) {
                    continue;
                }

                logger.error("node.getName()" + " contains invalid element " + child.getName());
            }
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
                node.getName() + sb.toString());
            StringBuilder b = new StringBuilder();
            if (parms.length > 0) {
                return factoryMethod.invoke(null, parms);
            }
            return factoryMethod.invoke(null, node);
        }
        catch (Exception e)
        {
            logger.error("Unable to invoke method " + factoryMethod.getName() + " in class " +
                clazz.getName() + " for element " + node.getName(), e);
        }
        return null;
    }

    private static void printArray(StringBuilder sb, Object[] array) {
        boolean first = true;
        for (Object obj : array) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(obj.toString());
        }
    }

    private static String getAttrValue(String name, Map<String, String>attrs) {
        for (String key : attrs.keySet()) {
            if (key.equalsIgnoreCase(name)) {
                String attr = attrs.get(key);
                attrs.remove(key);
                return attr;
            }
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
}