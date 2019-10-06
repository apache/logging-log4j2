package org.apache.logging.log4j.plugins.bind;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class MethodOptionBinder extends AbstractOptionBinder<Method> {

    public MethodOptionBinder(final Method method) {
        super(method, m -> m.getGenericParameterTypes()[0]);
    }

    @Override
    public Object bindObject(final Object target, final Object value) {
        Objects.requireNonNull(target);
        validate(value);
        try {
            element.invoke(target, value);
        } catch (final IllegalAccessException e) {
            throw new OptionBindingException(name, value, e);
        } catch (final InvocationTargetException e) {
            throw new OptionBindingException(name, value, e.getCause());
        }
        return target;
    }
}
