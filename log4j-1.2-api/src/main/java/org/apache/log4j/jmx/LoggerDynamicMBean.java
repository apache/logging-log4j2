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

import java.lang.reflect.Constructor;
import java.util.Enumeration;
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
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;

public class LoggerDynamicMBean extends AbstractDynamicMBean implements NotificationListener {

    // This Logger instance is for logging.
    private static final Logger cat = Logger.getLogger(LoggerDynamicMBean.class);
    private final MBeanConstructorInfo[] dConstructors = new MBeanConstructorInfo[1];

    private final MBeanOperationInfo[] dOperations = new MBeanOperationInfo[1];
    private final Vector dAttributes = new Vector();

    private final String dClassName = this.getClass().getName();

    private final String dDescription =
            "This MBean acts as a management facade for a org.apache.log4j.Logger instance.";

    // We wrap this Logger instance.
    private final Logger logger;

    public LoggerDynamicMBean(final Logger logger) {
        this.logger = logger;
        buildDynamicMBeanInfo();
    }

    void addAppender(final String appenderClass, final String appenderName) {
        cat.debug("addAppender called with " + appenderClass + ", " + appenderName);
        final Appender appender =
                (Appender) OptionConverter.instantiateByClassName(appenderClass, org.apache.log4j.Appender.class, null);
        appender.setName(appenderName);
        logger.addAppender(appender);

        // appenderMBeanRegistration();

    }

    void appenderMBeanRegistration() {
        final Enumeration enumeration = logger.getAllAppenders();
        while (enumeration.hasMoreElements()) {
            final Appender appender = (Appender) enumeration.nextElement();
            registerAppenderMBean(appender);
        }
    }

    private void buildDynamicMBeanInfo() {
        final Constructor[] constructors = this.getClass().getConstructors();
        dConstructors[0] = new MBeanConstructorInfo(
                "HierarchyDynamicMBean(): Constructs a HierarchyDynamicMBean instance", constructors[0]);

        dAttributes.add(
                new MBeanAttributeInfo("name", "java.lang.String", "The name of this Logger.", true, false, false));

        dAttributes.add(new MBeanAttributeInfo(
                "priority", "java.lang.String", "The priority of this logger.", true, true, false));

        final MBeanParameterInfo[] params = new MBeanParameterInfo[2];
        params[0] = new MBeanParameterInfo("class name", "java.lang.String", "add an appender to this logger");
        params[1] = new MBeanParameterInfo("appender name", "java.lang.String", "name of the appender");

        dOperations[0] = new MBeanOperationInfo(
                "addAppender", "addAppender(): add an appender", params, "void", MBeanOperationInfo.ACTION);
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

        // Check for a recognized attributeName and call the corresponding getter
        if (attributeName.equals("name")) {
            return logger.getName();
        } else if (attributeName.equals("priority")) {
            final Level l = logger.getLevel();
            if (l == null) {
                return null;
            } else {
                return l.toString();
            }
        } else if (attributeName.startsWith("appender=")) {
            try {
                return new ObjectName("log4j:" + attributeName);
            } catch (final MalformedObjectNameException e) {
                cat.error("Could not create ObjectName" + attributeName);
            } catch (final RuntimeException e) {
                cat.error("Could not create ObjectName" + attributeName);
            }
        }

        // If attributeName has not been recognized throw an AttributeNotFoundException
        throw (new AttributeNotFoundException("Cannot find " + attributeName + " attribute in " + dClassName));
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        // cat.debug("getMBeanInfo called.");

        final MBeanAttributeInfo[] attribs = new MBeanAttributeInfo[dAttributes.size()];
        dAttributes.toArray(attribs);

        final MBeanInfo mb = new MBeanInfo(
                dClassName, dDescription, attribs, dConstructors, dOperations, new MBeanNotificationInfo[0]);
        // cat.debug("getMBeanInfo exit.");
        return mb;
    }

    @Override
    public void handleNotification(final Notification notification, final Object handback) {
        cat.debug("Received notification: " + notification.getType());
        registerAppenderMBean((Appender) notification.getUserData());
    }

    @Override
    public Object invoke(final String operationName, final Object params[], final String signature[])
            throws MBeanException, ReflectionException {

        if (operationName.equals("addAppender")) {
            addAppender((String) params[0], (String) params[1]);
            return "Hello world.";
        }

        return null;
    }

    @Override
    public void postRegister(final java.lang.Boolean registrationDone) {
        appenderMBeanRegistration();
    }

    void registerAppenderMBean(final Appender appender) {
        final String name = getAppenderName(appender);
        cat.debug("Adding AppenderMBean for appender named " + name);
        ObjectName objectName = null;
        try {
            final AppenderDynamicMBean appenderMBean = new AppenderDynamicMBean(appender);
            objectName = new ObjectName("log4j", "appender", name);
            if (!server.isRegistered(objectName)) {
                registerMBean(appenderMBean, objectName);
                dAttributes.add(new MBeanAttributeInfo(
                        "appender=" + name,
                        "javax.management.ObjectName",
                        "The " + name + " appender.",
                        true,
                        true,
                        false));
            }

        } catch (final JMException e) {
            cat.error("Could not add appenderMBean for [" + name + "].", e);
        } catch (final java.beans.IntrospectionException e) {
            cat.error("Could not add appenderMBean for [" + name + "].", e);
        } catch (final RuntimeException e) {
            cat.error("Could not add appenderMBean for [" + name + "].", e);
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
        final Object value = attribute.getValue();

        if (name == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Attribute name cannot be null"),
                    "Cannot invoke the setter of " + dClassName + " with null attribute name");
        }

        if (name.equals("priority")) {
            if (value instanceof String) {
                final String s = (String) value;
                Level p = logger.getLevel();
                if (s.equalsIgnoreCase("NULL")) {
                    p = null;
                } else {
                    p = OptionConverter.toLevel(s, p);
                }
                logger.setLevel(p);
            }
        } else {
            throw (new AttributeNotFoundException(
                    "Attribute " + name + " not found in " + this.getClass().getName()));
        }
    }
}
