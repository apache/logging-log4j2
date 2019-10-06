package org.apache.logging.log4j.plugins.name;

import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.util.Strings;

import java.util.Optional;

public class PluginElementNameProvider implements AnnotatedElementNameProvider<PluginElement> {
    @Override
    public Optional<String> getSpecifiedName(final PluginElement annotation) {
        return Strings.trimToOptional(annotation.value());
    }
}
