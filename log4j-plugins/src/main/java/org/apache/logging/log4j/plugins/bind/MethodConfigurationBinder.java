package org.apache.logging.log4j.plugins.bind;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class MethodConfigurationBinder extends AbstractConfigurationBinder<Method> {

    public MethodConfigurationBinder(final Method method) {
        super(method, m -> m.getGenericParameterTypes()[0]);
    }

    @Override
    public Object bindObject(final Object target, final Object value) {
        Objects.requireNonNull(target);
        validate(value);
        try {
            element.invoke(target, value);
        } catch (final IllegalAccessException e) {
            throw new ConfigurationBindingException(name, value, e);
        } catch (final InvocationTargetException e) {
            throw new ConfigurationBindingException(name, value, e.getCause());
        }
        return target;
    }
}
