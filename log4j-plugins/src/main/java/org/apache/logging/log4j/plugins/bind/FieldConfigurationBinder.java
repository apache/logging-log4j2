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

import java.lang.reflect.Field;
import java.util.Objects;

public class FieldConfigurationBinder extends AbstractConfigurationBinder<Field> {

    public FieldConfigurationBinder(final Field field) {
        super(field, Field::getGenericType);
    }

    @Override
    public void bindObject(final Object factory, final Object value) {
        Objects.requireNonNull(factory);
        // FIXME: if we specify a default field value, @PluginAttribute's defaultType will override that
        if (value == null) {
            try {
                Object defaultValue = element.get(factory);
                validate(defaultValue);
                LOGGER.trace("Using default value {} for option {}", defaultValue, name);
            } catch (final IllegalAccessException e) {
                throw new ConfigurationBindingException("Unable to validate option " + name, e);
            }
        } else {
            validate(value);
            try {
                element.set(factory, value);
                LOGGER.trace("Using value {} for option {}", value, name);
            } catch (final IllegalAccessException e) {
                throw new ConfigurationBindingException(name, value, e);
            }
        }
    }

}
