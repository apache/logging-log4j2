package org.apache.logging.log4j.plugins.inject;

import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.util.NameUtil;
import org.apache.logging.log4j.util.StringBuilders;

public class PluginBuilderAttributeInjector extends AbstractConfigurationInjector<PluginBuilderAttribute, Object> {
    @Override
    public Object inject(final Object target) {
        return findAndRemoveNodeAttribute()
                .map(stringSubstitutionStrategy)
                .map(value -> {
                    String debugValue = annotation.sensitive() ? NameUtil.md5(value + getClass().getName()) : value;
                    StringBuilders.appendKeyDqValue(debugLog, name, debugValue);
                    return optionBinder.bindString(target, value);
                })
                .orElseGet(() -> optionBinder.bindObject(target, null));
    }
}
