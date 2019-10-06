package org.apache.logging.log4j.plugins.name;

import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.util.Strings;

import java.util.Optional;

public class PluginAttributeNameProvider implements AnnotatedElementNameProvider<PluginAttribute> {
    @Override
    public Optional<String> getSpecifiedName(final PluginAttribute annotation) {
        return Strings.trimToOptional(annotation.value());
    }
}
