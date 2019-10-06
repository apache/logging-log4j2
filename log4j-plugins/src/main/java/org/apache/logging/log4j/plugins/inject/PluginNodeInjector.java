package org.apache.logging.log4j.plugins.inject;

import org.apache.logging.log4j.plugins.PluginNode;
import org.apache.logging.log4j.plugins.util.TypeUtil;

public class PluginNodeInjector extends AbstractConfigurationInjector<PluginNode, Object> {
    @Override
    public Object inject(final Object target) {
        if (TypeUtil.isAssignable(conversionType, node.getClass())) {
            debugLog.append("Node=").append(node.getName());
            return optionBinder.bindObject(target, node);
        } else {
            LOGGER.error("Element with type {} annotated with @PluginNode not compatible with type {}.", conversionType, node.getClass());
            return target;
        }
    }
}
