package org.apache.logging.log4j.plugins.bind;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

// TODO: can support constructor factory following same pattern
public class FactoryMethodBinder {

    private final Method factoryMethod;
    private final Map<Parameter, OptionBinder> binders = new ConcurrentHashMap<>();
    private final Map<Parameter, Object> boundParameters = new ConcurrentHashMap<>();

    public FactoryMethodBinder(final Method factoryMethod) {
        this.factoryMethod = Objects.requireNonNull(factoryMethod);
        for (final Parameter parameter : factoryMethod.getParameters()) {
            binders.put(parameter, new ParameterOptionBinder(parameter));
        }
    }

    public void forEachParameter(final BiConsumer<Parameter, OptionBinder> consumer) {
        binders.forEach(consumer);
    }

    public Object invoke() throws Throwable {
        final Parameter[] parameters = factoryMethod.getParameters();
        final Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            args[i] = boundParameters.get(parameters[i]);
        }
        try {
            return factoryMethod.invoke(null, args);
        } catch (final IllegalAccessException e) {
            throw new OptionBindingException("Cannot access factory method " + factoryMethod, e);
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private class ParameterOptionBinder extends AbstractOptionBinder<Parameter> {
        private ParameterOptionBinder(final Parameter parameter) {
            super(parameter, Parameter::getParameterizedType);
        }

        @Override
        public Object bindObject(final Object target, final Object value) {
            validate(value);
            if (value != null) {
                boundParameters.put(element, value);
            }
            return target;
        }
    }
}
