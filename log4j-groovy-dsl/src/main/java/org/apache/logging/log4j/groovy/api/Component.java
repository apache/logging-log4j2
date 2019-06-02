package org.apache.logging.log4j.groovy.api;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.util.Strings;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Component {
    private final Map<String, String> attributes;
    private final List<Component> components;
    private final String pluginType;
    private final String value;

    Component(final Map<String, String> attributes, final List<Component> components, final String pluginType, final String value) {
        this.attributes = Collections.unmodifiableMap(attributes);
        this.components = Collections.unmodifiableList(components);
        this.pluginType = pluginType;
        this.value = value;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<Component> getComponents() {
        return components;
    }

    public String getPluginType() {
        return pluginType;
    }

    public String getValue() {
        return value;
    }

    public Node toNode(final Node parent, final PluginManager pluginManager) {
        PluginType<?> pluginType = pluginManager.getPluginType(this.pluginType);
        Node node = new Node(parent, this.pluginType, pluginType);
        node.getAttributes().putAll(this.attributes);
        node.setValue(this.value);
        for (Component component : this.components) {
            node.getChildren().add(component.toNode(node, pluginManager));
        }
        return node;
    }

    @Override
    public String toString() {
        return (pluginType != null ? pluginType : "configuration") + '(' + attributes.entrySet().stream().map(e -> e.getKey() + ": '" + e.getValue() + "'").collect(Collectors.joining(", ")) +
                ") {" + componentsToString() + "}\n";
    }

    private String componentsToString() {
        if (components.isEmpty()) return Strings.EMPTY;
        return components.stream().map(Component::toString).collect(Collectors.joining("\n", "\n", "\n"));
    }

    public static ComponentBuilder newBuilder() {
        return new ComponentBuilder();
    }

}
