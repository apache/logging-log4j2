package org.apache.logging.log4j.core.config.plugins.inject;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.util.TypeUtil;
import org.apache.logging.log4j.plugins.inject.AbstractConfigurationInjector;

public class PluginConfigurationInjector extends AbstractConfigurationInjector<PluginConfiguration, Configuration> {
    @Override
    public Object inject(final Object target) {
        if (TypeUtil.isAssignable(conversionType, configuration.getClass())) {
            debugLog.append("Configuration");
            if (configuration.getName() != null) {
                debugLog.append('(').append(configuration.getName()).append(')');
            }
            return optionBinder.bindObject(target, configuration);
        } else {
            LOGGER.warn("Element with type {} annotated with @PluginConfiguration is not compatible with type {}.",
                    conversionType, configuration.getClass());
            return target;
        }
    }
}
