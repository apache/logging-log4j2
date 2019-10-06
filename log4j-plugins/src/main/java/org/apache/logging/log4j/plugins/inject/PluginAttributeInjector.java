package org.apache.logging.log4j.plugins.inject;

import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.util.NameUtil;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PluginAttributeInjector extends AbstractConfigurationInjector<PluginAttribute, Object> {

    private static final Map<Type, Function<PluginAttribute, Object>> DEFAULT_VALUE_EXTRACTORS;

    static {
        final Map<Class<?>, Function<PluginAttribute, Object>> extractors = new ConcurrentHashMap<>();
        extractors.put(int.class, PluginAttribute::defaultInt);
        extractors.put(Integer.class, PluginAttribute::defaultInt);
        extractors.put(long.class, PluginAttribute::defaultLong);
        extractors.put(Long.class, PluginAttribute::defaultLong);
        extractors.put(boolean.class, PluginAttribute::defaultBoolean);
        extractors.put(Boolean.class, PluginAttribute::defaultBoolean);
        extractors.put(float.class, PluginAttribute::defaultFloat);
        extractors.put(Float.class, PluginAttribute::defaultFloat);
        extractors.put(double.class, PluginAttribute::defaultDouble);
        extractors.put(Double.class, PluginAttribute::defaultDouble);
        extractors.put(byte.class, PluginAttribute::defaultByte);
        extractors.put(Byte.class, PluginAttribute::defaultByte);
        extractors.put(char.class, PluginAttribute::defaultChar);
        extractors.put(Character.class, PluginAttribute::defaultChar);
        extractors.put(short.class, PluginAttribute::defaultShort);
        extractors.put(Short.class, PluginAttribute::defaultShort);
        extractors.put(Class.class, PluginAttribute::defaultClass);
        DEFAULT_VALUE_EXTRACTORS = Collections.unmodifiableMap(extractors);
    }

    @Override
    public Object inject(final Object target) {
        return findAndRemoveNodeAttribute()
                .map(stringSubstitutionStrategy)
                .map(value -> optionBinder.bindString(target, value))
                .orElseGet(() -> injectDefaultValue(target));
    }

    private Object injectDefaultValue(final Object target) {
        final Function<PluginAttribute, Object> extractor = DEFAULT_VALUE_EXTRACTORS.get(conversionType);
        if (extractor != null) {
            final Object value = extractor.apply(annotation);
            debugLog(value);
            return optionBinder.bindObject(target, value);
        }
        final String value = stringSubstitutionStrategy.apply(annotation.defaultString());
        if (Strings.isNotBlank(value)) {
            debugLog(value);
            return optionBinder.bindString(target, value);
        }
        return target;
    }

    private void debugLog(final Object value) {
        final Object debugValue = annotation.sensitive() ? NameUtil.md5(value + getClass().getName()) : value;
        StringBuilders.appendKeyDqValue(debugLog, name, debugValue);
    }

}
