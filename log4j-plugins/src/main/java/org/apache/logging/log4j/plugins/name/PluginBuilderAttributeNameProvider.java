package org.apache.logging.log4j.plugins.name;

import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.util.Strings;

import java.util.Optional;

public class PluginBuilderAttributeNameProvider implements AnnotatedElementNameProvider<PluginBuilderAttribute> {
    @Override
    public Optional<String> getSpecifiedName(final PluginBuilderAttribute annotation) {
        return Strings.trimToOptional(annotation.value());
    }
}
