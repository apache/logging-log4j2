/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.core.config.plugins;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.helpers.NameUtil;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Builder class to instantiate and configure a Plugin object using a PluginFactory method.
 *
 * @param <T> type of Plugin class.
 */
public class PluginBuilder<T> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final PluginType<T> pluginType;
    private final Class<T> clazz;

    private Configuration configuration;
    private Node node;
    private LogEvent event;

    private Method factory;
    private Annotation[][] annotations;
    private Class<?>[] types;
    private List<Node> children;
    private Collection<Node> used;

    /**
     * Constructs a PluginBuilder for a given PluginType.
     *
     * @param pluginType type of plugin to configure
     */
    public PluginBuilder(final PluginType<T> pluginType) {
        this.pluginType = pluginType;
        this.clazz = pluginType.getPluginClass();
    }

    /**
     * Specifies which annotation denotes a plugin factory method. The method must be static.
     *
     * @param annotationType class of annotation marking the plugin factory.
     * @param <A>            type of annotation.
     * @return {@code this}
     * @throws NoSuchMethodException
     */
    public <A extends Annotation> PluginBuilder<T> withFactoryMethodAnnotatedBy(final Class<A> annotationType)
            throws NoSuchMethodException {
        for (final Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(annotationType) && Modifier.isStatic(method.getModifiers())) {
                factory = method;
                LOGGER.trace("Using factory method {} on class {}", method.getName(), clazz.getName());
                return this;
            }
        }
        throw new NoSuchMethodException("No method annotated with " + annotationType.getName() + "was found in " + clazz.getName());
    }

    /**
     * Specifies the Configuration to use for constructing the plugin instance.
     *
     * @param configuration the configuration to use.
     * @return {@code this}
     */
    public PluginBuilder<T> withConfiguration(final Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * Specifies the Node corresponding to the plugin object that will be created.
     *
     * @param node the plugin configuration node to use.
     * @return {@code this}
     */
    public PluginBuilder<T> withConfigurationNode(final Node node) {
        this.node = node;
        this.children = this.node.getChildren();
        this.used = new HashSet<Node>(this.children.size());
        return this;
    }

    /**
     * Specifies the LogEvent that may be used to provide extra context for string substitutions.
     *
     * @param event the event to use for extra information.
     * @return {@code this}
     */
    public PluginBuilder<T> forLogEvent(final LogEvent event) {
        this.event = event;
        return this;
    }

    /**
     * Builds the plugin object.
     *
     * @return the plugin object or {@code null} if there was a problem creating it.
     */
    public Object build() {
        init();
        try {
            return factory.invoke(null, generateParameters());
        } catch (final Exception e) {
            LOGGER.error("Unable to invoke method {} in class {} for element {}",
                    factory.getName(), clazz.getName(), node.getName(), e);
            return null;
        }
    }

    private void init() {
        if (factory == null) {
            throw new IllegalStateException("No factory method was found.");
        }
        if (configuration == null) {
            throw new IllegalStateException("No Configuration object was set.");
        }
        if (node == null) {
            throw new IllegalStateException("No Node object was set.");
        }
        annotations = factory.getParameterAnnotations();
        types = factory.getParameterTypes();
    }

    private Object[] generateParameters() {
        final StringBuilder sb = new StringBuilder();
        final Object[] args = new Object[annotations.length];
        for (int i = 0; i < annotations.length; i++) {
            final String[] aliases = extractPluginAliases(annotations[i]);
            for (Annotation a : annotations[i]) {
                if (a instanceof PluginAliases) {
                    continue; // already processed
                }
                sb.append(sb.length() == 0 ? "with params(" : ", ");
                if (a instanceof PluginNode) {
                    args[i] = node;
                    sb.append("Node=").append(node.getName());
                } else if (a instanceof PluginConfiguration) {
                    args[i] = configuration;
                    sb.append("Configuration");
                    if (configuration.getName() != null) {
                        sb.append('(').append(configuration.getName()).append(')');
                    }
                } else if (a instanceof PluginValue) {
                    final String name = ((PluginValue) a).value();
                    final String v = node.getValue() != null ? node.getValue() : getAttrValue("value");
                    final String value = configuration.getStrSubstitutor().replace(event, v);
                    args[i] = value;
                    sb.append(name).append("=\"").append(value).append('"');
                } else if (a instanceof PluginAttribute) {
                    final PluginAttribute attribute = (PluginAttribute) a;
                    final String name = attribute.value();
                    args[i] = getReplacedAttributeValue(name, aliases);
                    sb.append(name).append("=\"").append(args[i]).append('"');
                } else if (a instanceof SensitivePluginAttribute) {
                    // LOG4J2-605
                    // we shouldn't be displaying passwords
                    final SensitivePluginAttribute attribute = (SensitivePluginAttribute) a;
                    final String name = attribute.value();
                    args[i] = getReplacedAttributeValue(name, aliases);
                    sb.append(name).append("=\"").append(NameUtil.md5(args[i] + PluginBuilder.class.getName())).append('"');
                } else if (a instanceof PluginElement) {
                    final PluginElement element = (PluginElement) a;
                    final String name = element.value();
                    if (types[i].isArray()) {
                        final Class<?> type = types[i].getComponentType();
                        sb.append(name).append("={");
                        final List<Object> values = new ArrayList<Object>();
                        boolean first = true;
                        for (final Node child : children) {
                            final PluginType<?> childType = child.getType();
                            if (name.equalsIgnoreCase(childType.getElementName()) ||
                                    type.isAssignableFrom(childType.getPluginClass())) {
                                if (!first) {
                                    sb.append(", ");
                                }
                                first = false;
                                used.add(child);
                                final Object o = child.getObject();
                                if (o == null) {
                                    LOGGER.error("Null object returned for {} in {}", child.getName(), node.getName());
                                    continue;
                                }
                                if (o.getClass().isArray()) {
                                    sb.append(Arrays.toString((Object[]) o));
                                    args[i] = o;
                                    break;
                                }
                                sb.append(child.toString());
                                values.add(o);
                            }
                        }
                        sb.append('}');
                        if (args[i] != null) {
                            break;
                        }
                        if (!(values.isEmpty() || type.isAssignableFrom(values.get(0).getClass()))) {
                            LOGGER.error(
                                    "Attempted to assign List containing class {} to array of type {} for attribute {}",
                                    values.get(0).getClass().getName(), type, name
                            );
                            break;
                        }
                        args[i] = collectionToArray(values, type);
                    } else {
                        final Node namedNode = findNamedNode(name, types[i], children);
                        if (namedNode == null) {
                            sb.append("null");
                        } else {
                            sb.append(namedNode.getName()).append('(').append(namedNode.toString()).append(')');
                            used.add(namedNode);
                            args[i] = namedNode.getObject();
                        }
                    }
                }
            }
        }
        if (sb.length() > 0) {
            sb.append(')');
        }
        checkForRemainingAttributes();
        verifyNodeChildrenUsed();
        LOGGER.debug("Calling {} on class {} for element {} {}", factory.getName(), clazz.getName(), node.getName(), sb.toString());
        return args;
    }

    private static String[] extractPluginAliases(final Annotation... parmTypes) {
        String[] aliases = null;
        for (final Annotation a : parmTypes) {
            if (a instanceof PluginAliases) {
                aliases = ((PluginAliases) a).value();
            }
        }
        return aliases;
    }

    private String getReplacedAttributeValue(final String name, final String... aliases) {
        return configuration.getStrSubstitutor().replace(event, getAttrValue(name, aliases));
    }

    private String getAttrValue(final String name, final String... aliases) {
        final Map<String, String> attrs = node.getAttributes();
        for (final Map.Entry<String, String> entry : attrs.entrySet()) {
            final String key = entry.getKey();
            if (key.equalsIgnoreCase(name)) {
                final String attr = entry.getValue();
                attrs.remove(key);
                return attr;
            }
            if (aliases != null) {
                for (final String alias : aliases) {
                    if (key.equalsIgnoreCase(alias)) {
                        final String attr = entry.getValue();
                        attrs.remove(key);
                        return attr;
                    }
                }
            }
        }
        return null;
    }

    private static Object[] collectionToArray(final Collection<?> collection, final Class<?> type) {
        final Object[] array = (Object[]) Array.newInstance(type, collection.size());
        int i = 0;
        for (final Object obj : collection) {
            array[i] = obj;
            ++i;
        }
        return array;
    }

    private static Node findNamedNode(final String name, final Class<?> type, final Iterable<Node> nodes) {
        for (final Node child : nodes) {
            final PluginType<?> childType = child.getType();
            if (name.equalsIgnoreCase(childType.getElementName()) ||
                    type.isAssignableFrom(childType.getPluginClass())) {
                return child;
            }
        }
        return null;
    }

    private void checkForRemainingAttributes() {
        final Map<String, String> attrs = node.getAttributes();
        if (!attrs.isEmpty()) {
            final StringBuilder eb = new StringBuilder();
            for (final String key : attrs.keySet()) {
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
                eb.append('"');
                eb.append(key);
                eb.append('"');

            }
            LOGGER.error(eb.toString());
        }
    }

    private void verifyNodeChildrenUsed() {
        if (!(pluginType.isDeferChildren() || used.size() == children.size())) {
            children.removeAll(used);
            for (final Node child : children) {
                final String nodeType = node.getType().getElementName();
                final String start = nodeType.equals(node.getName()) ? node.getName() : nodeType + ' ' + node.getName();
                LOGGER.error("{} has no parameter that matches element {}", start, child.getName());
            }
        }
    }
}
