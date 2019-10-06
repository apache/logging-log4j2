package org.apache.logging.log4j.plugins.name;

import org.apache.logging.log4j.plugins.PluginValue;
import org.apache.logging.log4j.util.Strings;

import java.util.Optional;

public class PluginValueNameProvider implements AnnotatedElementNameProvider<PluginValue> {
    @Override
    public Optional<String> getSpecifiedName(final PluginValue annotation) {
        return Strings.trimToOptional(annotation.value());
    }
}
