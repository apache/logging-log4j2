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
package org.apache.log4j.config;

import java.beans.PropertyDescriptor;
import java.util.Properties;

/**
 *
 * @since 1.1
 */
public class PropertySetter {

    /**
     * Create a new PropertySetter for the specified Object. This is done
     * in preparation for invoking {@link #setProperty} one or more times.
     *
     * @param obj  the object for which to set properties
     */
    public PropertySetter(final Object obj) {
    }


    /**
     * Set the properties for the object that match the <code>prefix</code> passed as parameter.
     *
     * @param properties The properties
     * @param prefix The prefix
     */
    public void setProperties(final Properties properties, final String prefix) {
    }

    /**
     * Set a property on this PropertySetter's Object. If successful, this
     * method will invoke a setter method on the underlying Object. The
     * setter is the one for the specified property name and the value is
     * determined partly from the setter argument type and partly from the
     * value specified in the call to this method.
     *
     * <p>If the setter expects a String no conversion is necessary.
     * If it expects an int, then an attempt is made to convert 'value'
     * to an int using new Integer(value). If the setter expects a boolean,
     * the conversion is by new Boolean(value).
     *
     * @param name    name of the property
     * @param value   String value of the property
     */
    public void setProperty(final String name, final String value) {
    }

    /**
     * Set the named property given a {@link PropertyDescriptor}.
     *
     * @param prop A PropertyDescriptor describing the characteristics of the property to set.
     * @param name The named of the property to set.
     * @param value The value of the property.
     * @throws PropertySetterException (Never actually throws this exception. Kept for historical purposes.)
     */
    public void setProperty(final PropertyDescriptor prop, final String name, final String value)
        throws PropertySetterException {
    }

    /**
     * Set the properties of an object passed as a parameter in one
     * go. The <code>properties</code> are parsed relative to a
     * <code>prefix</code>.
     *
     * @param obj The object to configure.
     * @param properties A java.util.Properties containing keys and values.
     * @param prefix Only keys having the specified prefix will be set.
     */
    public static void setProperties(final Object obj, final Properties properties, final String prefix) {
    }
}
