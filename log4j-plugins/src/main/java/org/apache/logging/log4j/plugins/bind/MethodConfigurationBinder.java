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
import java.util.Objects;

public class MethodConfigurationBinder extends AbstractConfigurationBinder<Method> {

    public MethodConfigurationBinder(final Method method) {
        super(method, m -> m.getGenericParameterTypes()[0]);
    }

    @Override
    public void bindObject(final Object factory, final Object value) {
        Objects.requireNonNull(factory);
        validate(value);
        try {
            element.invoke(factory, value);
        } catch (final IllegalAccessException e) {
            throw new ConfigurationBindingException(name, value, e);
        } catch (final InvocationTargetException e) {
            throw new ConfigurationBindingException(name, value, e.getCause());
        }
    }
}
