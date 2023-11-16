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
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.OptionHandler;

public class AppenderDynamicMBean extends AbstractDynamicMBean {

    // This category instance is for logging.
    private static final Logger cat = Logger.getLogger(AppenderDynamicMBean.class);
    private final MBeanConstructorInfo[] dConstructors = new MBeanConstructorInfo[1];
    private final Vector dAttributes = new Vector();

    private final String dClassName = this.getClass().getName();
    private final Hashtable dynamicProps = new Hashtable(5);
    private final MBeanOperationInfo[] dOperations = new MBeanOperationInfo[2];

    private final String dDescription = "This MBean acts as a management facade for log4j appenders.";

    // We wrap this appender instance.
    private final Appender appender;

    public AppenderDynamicMBean(final Appender appender) throws IntrospectionException {
        this.appender = appender;
        buildDynamicMBeanInfo();
    }

    private void buildDynamicMBeanInfo() throws IntrospectionException {
        final Constructor[] constructors = this.getClass().getConstructors();
        dConstructors[0] = new MBeanConstructorInfo(
                "AppenderDynamicMBean(): Constructs a AppenderDynamicMBean instance", constructors[0]);

        final BeanInfo bi = Introspector.getBeanInfo(appender.getClass());
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
                    if (returnClass.isAssignableFrom(Priority.class)) {
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

        MBeanParameterInfo[] params = new MBeanParameterInfo[0];

        dOperations[0] = new MBeanOperationInfo(
                "activateOptions", "activateOptions(): add an appender", params, "void", MBeanOperationInfo.ACTION);

        params = new MBeanParameterInfo[1];
        params[0] = new MBeanParameterInfo("layout class", "java.lang.String", "layout class");

        dOperations[1] = new MBeanOperationInfo(
                "setLayout", "setLayout(): add a layout", params, "void", MBeanOperationInfo.ACTION);
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

        cat.debug("getAttribute called with [" + attributeName + "].");
        if (attributeName.startsWith("appender=" + appender.getName() + ",layout")) {
            try {
                return new ObjectName("log4j:" + attributeName);
            } catch (final MalformedObjectNameException e) {
                cat.error("attributeName", e);
            } catch (final RuntimeException e) {
                cat.error("attributeName", e);
            }
        }

        final MethodUnion mu = (MethodUnion) dynamicProps.get(attributeName);

        // cat.debug("----name="+attributeName+", b="+b);

        if (mu != null && mu.readMethod != null) {
            try {
                return mu.readMethod.invoke(appender, null);
            } catch (final IllegalAccessException e) {
                return null;
            } catch (final InvocationTargetException e) {
                if (e.getTargetException() instanceof InterruptedException
                        || e.getTargetException() instanceof InterruptedIOException) {
                    Thread.currentThread().interrupt();
                }
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

        if (operationName.equals("activateOptions") && appender instanceof OptionHandler) {
            final OptionHandler oh = (OptionHandler) appender;
            oh.activateOptions();
            return "Options activated.";
        } else if (operationName.equals("setLayout")) {
            final Layout layout =
                    (Layout) OptionConverter.instantiateByClassName((String) params[0], Layout.class, null);
            appender.setLayout(layout);
            registerLayoutMBean(layout);
        }
        return null;
    }

    private boolean isSupportedType(final Class clazz) {
        if (clazz.isPrimitive() || (clazz == String.class) || clazz.isAssignableFrom(Priority.class)) {
            return true;
        }

        return false;
    }

    @Override
    public ObjectName preRegister(final MBeanServer server, final ObjectName name) {
        cat.debug("preRegister called. Server=" + server + ", name=" + name);
        this.server = server;
        registerLayoutMBean(appender.getLayout());

        return name;
    }

    void registerLayoutMBean(final Layout layout) {
        if (layout == null) {
            return;
        }

        final String name =
                getAppenderName(appender) + ",layout=" + layout.getClass().getName();
        cat.debug("Adding LayoutMBean:" + name);
        ObjectName objectName = null;
        try {
            final LayoutDynamicMBean appenderMBean = new LayoutDynamicMBean(layout);
            objectName = new ObjectName("log4j:appender=" + name);
            if (!server.isRegistered(objectName)) {
                registerMBean(appenderMBean, objectName);
                dAttributes.add(new MBeanAttributeInfo(
                        "appender=" + name,
                        "javax.management.ObjectName",
                        "The " + name + " layout.",
                        true,
                        true,
                        false));
            }

        } catch (final JMException e) {
            cat.error("Could not add DynamicLayoutMBean for [" + name + "].", e);
        } catch (final java.beans.IntrospectionException e) {
            cat.error("Could not add DynamicLayoutMBean for [" + name + "].", e);
        } catch (final RuntimeException e) {
            cat.error("Could not add DynamicLayoutMBean for [" + name + "].", e);
        }
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
                mu.writeMethod.invoke(appender, o);

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
        } else if (name.endsWith(".layout")) {

        } else {
            throw (new AttributeNotFoundException(
                    "Attribute " + name + " not found in " + this.getClass().getName()));
        }
    }
}
