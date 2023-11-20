/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
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
package org.apache.log4j.jmx;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.InterruptedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Vector;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.OptionHandler;

public class LayoutDynamicMBean extends AbstractDynamicMBean {

    // This category instance is for logging.
    private static final Logger cat = Logger.getLogger(LayoutDynamicMBean.class);
    private final MBeanConstructorInfo[] dConstructors = new MBeanConstructorInfo[1];
    private final Vector dAttributes = new Vector();

    private final String dClassName = this.getClass().getName();
    private final Hashtable dynamicProps = new Hashtable(5);
    private final MBeanOperationInfo[] dOperations = new MBeanOperationInfo[1];

    private final String dDescription = "This MBean acts as a management facade for log4j layouts.";

    // We wrap this layout instance.
    private final Layout layout;

    public LayoutDynamicMBean(final Layout layout) throws IntrospectionException {
        this.layout = layout;
        buildDynamicMBeanInfo();
    }

    private void buildDynamicMBeanInfo() throws IntrospectionException {
        final Constructor[] constructors = this.getClass().getConstructors();
        dConstructors[0] = new MBeanConstructorInfo(
                "LayoutDynamicMBean(): Constructs a LayoutDynamicMBean instance", constructors[0]);

        final BeanInfo bi = Introspector.getBeanInfo(layout.getClass());
        final PropertyDescriptor[] pd = bi.getPropertyDescriptors();

        final int size = pd.length;

        for (int i = 0; i < size; i++) {
            final String name = pd[i].getName();
            final Method readMethod = pd[i].getReadMethod();
            final Method writeMethod = pd[i].getWriteMethod();
            if (readMethod != null) {
                final Class returnClass = readMethod.getReturnType();
                if (isSupportedType(returnClass)) {
                    String returnClassName;
                    if (returnClass.isAssignableFrom(Level.class)) {
                        returnClassName = "java.lang.String";
                    } else {
                        returnClassName = returnClass.getName();
                    }

                    dAttributes.add(
                            new MBeanAttributeInfo(name, returnClassName, "Dynamic", true, writeMethod != null, false));
                    dynamicProps.put(name, new MethodUnion(readMethod, writeMethod));
                }
            }
        }

        final MBeanParameterInfo[] params = new MBeanParameterInfo[0];

        dOperations[0] = new MBeanOperationInfo(
                "activateOptions", "activateOptions(): add an layout", params, "void", MBeanOperationInfo.ACTION);
    }

    @Override
    public Object getAttribute(final String attributeName)
            throws AttributeNotFoundException, MBeanException, ReflectionException {

        // Check attributeName is not null to avoid NullPointerException later on
        if (attributeName == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Attribute name cannot be null"),
                    "Cannot invoke a getter of " + dClassName + " with null attribute name");
        }

        final MethodUnion mu = (MethodUnion) dynamicProps.get(attributeName);

        cat.debug("----name=" + attributeName + ", mu=" + mu);

        if (mu != null && mu.readMethod != null) {
            try {
                return mu.readMethod.invoke(layout, null);
            } catch (final InvocationTargetException e) {
                if (e.getTargetException() instanceof InterruptedException
                        || e.getTargetException() instanceof InterruptedIOException) {
                    Thread.currentThread().interrupt();
                }
                return null;
            } catch (final IllegalAccessException e) {
                return null;
            } catch (final RuntimeException e) {
                return null;
            }
        }

        // If attributeName has not been recognized throw an AttributeNotFoundException
        throw (new AttributeNotFoundException("Cannot find " + attributeName + " attribute in " + dClassName));
    }

    @Override
    protected Logger getLogger() {
        return cat;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        cat.debug("getMBeanInfo called.");

        final MBeanAttributeInfo[] attribs = new MBeanAttributeInfo[dAttributes.size()];
        dAttributes.toArray(attribs);

        return new MBeanInfo(
                dClassName, dDescription, attribs, dConstructors, dOperations, new MBeanNotificationInfo[0]);
    }

    @Override
    public Object invoke(final String operationName, final Object params[], final String signature[])
            throws MBeanException, ReflectionException {

        if (operationName.equals("activateOptions") && layout instanceof OptionHandler) {
            final OptionHandler oh = (OptionHandler) layout;
            oh.activateOptions();
            return "Options activated.";
        }
        return null;
    }

    private boolean isSupportedType(final Class clazz) {
        if (clazz.isPrimitive() || (clazz == String.class) || clazz.isAssignableFrom(Level.class)) {
            return true;
        }

        return false;
    }

    @Override
    public void setAttribute(final Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {

        // Check attribute is not null to avoid NullPointerException later on
        if (attribute == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Attribute cannot be null"),
                    "Cannot invoke a setter of " + dClassName + " with null attribute");
        }
        final String name = attribute.getName();
        Object value = attribute.getValue();

        if (name == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Attribute name cannot be null"),
                    "Cannot invoke the setter of " + dClassName + " with null attribute name");
        }

        final MethodUnion mu = (MethodUnion) dynamicProps.get(name);

        if (mu != null && mu.writeMethod != null) {
            final Object[] o = new Object[1];

            final Class[] params = mu.writeMethod.getParameterTypes();
            if (params[0] == org.apache.log4j.Priority.class) {
                value = OptionConverter.toLevel((String) value, (Level) getAttribute(name));
            }
            o[0] = value;

            try {
                mu.writeMethod.invoke(layout, o);

            } catch (final InvocationTargetException e) {
                if (e.getTargetException() instanceof InterruptedException
                        || e.getTargetException() instanceof InterruptedIOException) {
                    Thread.currentThread().interrupt();
                }
                cat.error("FIXME", e);
            } catch (final IllegalAccessException e) {
                cat.error("FIXME", e);
            } catch (final RuntimeException e) {
                cat.error("FIXME", e);
            }
        } else {
            throw (new AttributeNotFoundException(
                    "Attribute " + name + " not found in " + this.getClass().getName()));
        }
    }
}
