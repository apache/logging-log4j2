package org.apache.logging.log4j.groovy.api;

import org.apache.logging.log4j.core.util.Builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ComponentBuilder implements Builder<Component> {
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<Component> components = new ArrayList<>();
    private String pluginType;
    private String value;

    public ComponentBuilder withName(final String name) {
        return withAttribute("name", name);
    }

    public ComponentBuilder withAttribute(final String attributeName, final String attributeValue) {
        attributes.put(attributeName, attributeValue);
        return this;
    }

    public ComponentBuilder withAttributes(final Map<String, String> attributes) {
        this.attributes.putAll(attributes);
        return this;
    }

    public ComponentBuilder withComponent(final Component component) {
        components.add(component);
        return this;
    }

    public ComponentBuilder withComponent(final Builder<Component> componentBuilder) {
        components.add(componentBuilder.build());
        return this;
    }

    public ComponentBuilder withPluginType(final String pluginType) {
        this.pluginType = pluginType;
        return this;
    }

    public ComponentBuilder withValue(final String value) {
        this.value = value;
        return this;
    }

    @Override
    public Component build() {
        return new Component(attributes, components, pluginType, value);
    }
}
