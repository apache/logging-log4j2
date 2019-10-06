package org.apache.logging.log4j.plugins.inject;

import org.apache.logging.log4j.plugins.PluginValue;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

public class PluginValueInjector extends AbstractConfigurationInjector<PluginValue, Object> {
    @Override
    public Object inject(final Object target) {
        final String elementValue = node.getValue();
        final String attributeValue = node.getAttributes().get(name);
        String rawValue = null; // if neither is specified, return null (LOG4J2-1313)
        if (Strings.isNotEmpty(elementValue)) {
            if (Strings.isNotEmpty(attributeValue)) {
                LOGGER.error("Configuration contains {} with both attribute value ({}) AND element" +
                                " value ({}). Please specify only one value. Using the element value.",
                        node.getName(), attributeValue, elementValue);
            }
            rawValue = elementValue;
        } else {
            rawValue = findAndRemoveNodeAttribute().orElse(null);
        }
        final String value = stringSubstitutionStrategy.apply(rawValue);
        StringBuilders.appendKeyDqValue(debugLog, name, value);
        return configurationBinder.bindString(target, value);
    }
}
