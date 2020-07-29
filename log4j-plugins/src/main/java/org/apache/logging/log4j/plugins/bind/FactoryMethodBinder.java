/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

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
    private final Map<Parameter, ConfigurationBinder> binders = new ConcurrentHashMap<>();
    private final Map<Parameter, Object> boundParameters = new ConcurrentHashMap<>();

    public FactoryMethodBinder(final Method factoryMethod) {
        this.factoryMethod = Objects.requireNonNull(factoryMethod);
        for (final Parameter parameter : factoryMethod.getParameters()) {
            binders.put(parameter, new ParameterConfigurationBinder(parameter));
        }
    }

    public void forEachParameter(final BiConsumer<Parameter, ConfigurationBinder> consumer) {
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
            throw new ConfigurationBindingException("Cannot access factory method " + factoryMethod, e);
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private class ParameterConfigurationBinder extends AbstractConfigurationBinder<Parameter> {
        private ParameterConfigurationBinder(final Parameter parameter) {
            super(parameter, Parameter::getParameterizedType);
        }

        @Override
        public void bindObject(final Object factory, final Object value) {
            validate(value);
            if (value != null) {
                boundParameters.put(element, value);
            }
        }
    }
}
