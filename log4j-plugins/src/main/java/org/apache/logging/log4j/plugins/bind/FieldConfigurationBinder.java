package org.apache.logging.log4j.plugins.bind;

import java.lang.reflect.Field;
import java.util.Objects;

public class FieldConfigurationBinder extends AbstractConfigurationBinder<Field> {

    public FieldConfigurationBinder(final Field field) {
        super(field, Field::getGenericType);
    }

    @Override
    public Object bindObject(final Object target, final Object value) {
        Objects.requireNonNull(target);
        // FIXME: if we specify a default field value, @PluginAttribute's defaultType will override that
        if (value == null) {
            try {
                Object defaultValue = element.get(target);
                validate(defaultValue);
                LOGGER.trace("Using default value {} for option {}", defaultValue, name);
            } catch (final IllegalAccessException e) {
                throw new ConfigurationBindingException("Unable to validate option " + name, e);
            }
            return target;
        }
        validate(value);
        try {
            element.set(target, value);
            LOGGER.trace("Using value {} for option {}", value, name);
            return target;
        } catch (final IllegalAccessException e) {
            throw new ConfigurationBindingException(name, value, e);
        }
    }

}
