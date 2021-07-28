/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Contributors:  Georg Lundesgaard

package org.apache.log4j.config;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.OptionHandler;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.OptionConverter;
import org.apache.logging.log4j.status.StatusLogger;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * General purpose Object property setter. Clients repeatedly invokes
 * {@link #setProperty setProperty(name,value)} in order to invoke setters
 * on the Object specified in the constructor. This class relies on the
 * JavaBeans {@link Introspector} to analyze the given Object Class using
 * reflection.
 *
 * <p>Usage:
 * <pre>
 * PropertySetter ps = new PropertySetter(anObject);
 * ps.set("name", "Joe");
 * ps.set("age", "32");
 * ps.set("isMale", "true");
 * </pre>
 * will cause the invocations anObject.setName("Joe"), anObject.setAge(32),
 * and setMale(true) if such methods exist with those signatures.
 * Otherwise an {@link IntrospectionException} are thrown.
 */
public class PropertySetter {
    private static final PropertyDescriptor[] EMPTY_PROPERTY_DESCRIPTOR_ARRAY = {};
    private static Logger LOGGER = StatusLogger.getLogger();
    protected Object obj;
    protected PropertyDescriptor[] props;

    /**
     * Create a new PropertySetter for the specified Object. This is done
     * in prepartion for invoking {@link #setProperty} one or more times.
     *
     * @param obj the object for which to set properties
     */
    public PropertySetter(Object obj) {
        this.obj = obj;
    }

    /**
     * Set the properties of an object passed as a parameter in one
     * go. The <code>properties</code> are parsed relative to a
     * <code>prefix</code>.
     *
     * @param obj        The object to configure.
     * @param properties A java.util.Properties containing keys and values.
     * @param prefix     Only keys having the specified prefix will be set.
     */
    public static void setProperties(Object obj, Properties properties, String prefix) {
        new PropertySetter(obj).setProperties(properties, prefix);
    }

    /**
     * Uses JavaBeans {@link Introspector} to computer setters of object to be
     * configured.
     */
    protected void introspect() {
        try {
            BeanInfo bi = Introspector.getBeanInfo(obj.getClass());
            props = bi.getPropertyDescriptors();
        } catch (IntrospectionException ex) {
            LOGGER.error("Failed to introspect {}: {}", obj, ex.getMessage());
            props = EMPTY_PROPERTY_DESCRIPTOR_ARRAY;
        }
    }

    /**
     * Set the properites for the object that match the
     * <code>prefix</code> passed as parameter.
     * @param properties The properties.
     * @param prefix The prefix of the properties to use.
     */
    public void setProperties(Properties properties, String prefix) {
        int len = prefix.length();

        for (String key : properties.stringPropertyNames()) {

            // handle only properties that start with the desired prefix.
            if (key.startsWith(prefix)) {


                // ignore key if it contains dots after the prefix
                if (key.indexOf('.', len + 1) > 0) {
                    continue;
                }

                String value = OptionConverter.findAndSubst(key, properties);
                key = key.substring(len);
                if (("layout".equals(key) || "errorhandler".equals(key)) && obj instanceof Appender) {
                    continue;
                }
                //
                //   if the property type is an OptionHandler
                //     (for example, triggeringPolicy of org.apache.log4j.rolling.RollingFileAppender)
                PropertyDescriptor prop = getPropertyDescriptor(Introspector.decapitalize(key));
                if (prop != null
                        && OptionHandler.class.isAssignableFrom(prop.getPropertyType())
                        && prop.getWriteMethod() != null) {
                    OptionHandler opt = (OptionHandler)
                            OptionConverter.instantiateByKey(properties, prefix + key,
                                    prop.getPropertyType(),
                                    null);
                    PropertySetter setter = new PropertySetter(opt);
                    setter.setProperties(properties, prefix + key + ".");
                    try {
                        prop.getWriteMethod().invoke(this.obj, opt);
                    } catch (InvocationTargetException ex) {
                        if (ex.getTargetException() instanceof InterruptedException
                                || ex.getTargetException() instanceof InterruptedIOException) {
                            Thread.currentThread().interrupt();
                        }
                        LOGGER.warn("Failed to set property [{}] to value \"{}\".", key, value, ex);
                    } catch (IllegalAccessException | RuntimeException ex) {
                        LOGGER.warn("Failed to set property [{}] to value \"{}\".", key, value, ex);
                    }
                    continue;
                }

                setProperty(key, value);
            }
        }
        activate();
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
     * @param name  name of the property
     * @param value String value of the property
     */
    public void setProperty(String name, String value) {
        if (value == null) {
            return;
        }

        name = Introspector.decapitalize(name);
        PropertyDescriptor prop = getPropertyDescriptor(name);

        //LOGGER.debug("---------Key: "+name+", type="+prop.getPropertyType());

        if (prop == null) {
            LOGGER.warn("No such property [" + name + "] in " +
                    obj.getClass().getName() + ".");
        } else {
            try {
                setProperty(prop, name, value);
            } catch (PropertySetterException ex) {
                LOGGER.warn("Failed to set property [{}] to value \"{}\".", name, value, ex.rootCause);
            }
        }
    }

    /**
     * Set the named property given a {@link PropertyDescriptor}.
     *
     * @param prop  A PropertyDescriptor describing the characteristics
     *              of the property to set.
     * @param name  The named of the property to set.
     * @param value The value of the property.
     * @throws PropertySetterException if no setter is available.
     */
    public void setProperty(PropertyDescriptor prop, String name, String value)
            throws PropertySetterException {
        Method setter = prop.getWriteMethod();
        if (setter == null) {
            throw new PropertySetterException("No setter for property [" + name + "].");
        }
        Class<?>[] paramTypes = setter.getParameterTypes();
        if (paramTypes.length != 1) {
            throw new PropertySetterException("#params for setter != 1");
        }

        Object arg;
        try {
            arg = convertArg(value, paramTypes[0]);
        } catch (Throwable t) {
            throw new PropertySetterException("Conversion to type [" + paramTypes[0] +
                    "] failed. Reason: " + t);
        }
        if (arg == null) {
            throw new PropertySetterException(
                    "Conversion to type [" + paramTypes[0] + "] failed.");
        }
        LOGGER.debug("Setting property [" + name + "] to [" + arg + "].");
        try {
            setter.invoke(obj, arg);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof InterruptedException
                    || ex.getTargetException() instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            throw new PropertySetterException(ex);
        } catch (IllegalAccessException | RuntimeException ex) {
            throw new PropertySetterException(ex);
        }
    }


    /**
     * Convert <code>val</code> a String parameter to an object of a
     * given type.
     * @param val The value to convert.
     * @param type The type of the value to convert to.
     * @return The result of the conversion.
     */
    protected Object convertArg(String val, Class<?> type) {
        if (val == null) {
            return null;
        }

        String v = val.trim();
        if (String.class.isAssignableFrom(type)) {
            return val;
        } else if (Integer.TYPE.isAssignableFrom(type)) {
            return Integer.parseInt(v);
        } else if (Long.TYPE.isAssignableFrom(type)) {
            return Long.parseLong(v);
        } else if (Boolean.TYPE.isAssignableFrom(type)) {
            if ("true".equalsIgnoreCase(v)) {
                return Boolean.TRUE;
            } else if ("false".equalsIgnoreCase(v)) {
                return Boolean.FALSE;
            }
        } else if (Priority.class.isAssignableFrom(type)) {
            return org.apache.log4j.helpers.OptionConverter.toLevel(v, Level.DEBUG);
        } else if (ErrorHandler.class.isAssignableFrom(type)) {
            return OptionConverter.instantiateByClassName(v,
                    ErrorHandler.class, null);
        }
        return null;
    }


    protected PropertyDescriptor getPropertyDescriptor(String name) {
        if (props == null) {
            introspect();
        }
        for (PropertyDescriptor prop : props) {
            if (name.equals(prop.getName())) {
                return prop;
            }
        }
        return null;
    }

    public void activate() {
        if (obj instanceof OptionHandler) {
            ((OptionHandler) obj).activateOptions();
        }
    }
}
