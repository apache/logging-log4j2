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
import java.util.Vector;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggerRepository;

public class HierarchyDynamicMBean extends AbstractDynamicMBean
        implements HierarchyEventListener, NotificationBroadcaster {

    static final String ADD_APPENDER = "addAppender.";
    static final String THRESHOLD = "threshold";

    private static final Logger log = Logger.getLogger(HierarchyDynamicMBean.class);
    private final MBeanConstructorInfo[] dConstructors = new MBeanConstructorInfo[1];

    private final MBeanOperationInfo[] dOperations = new MBeanOperationInfo[1];
    private final Vector vAttributes = new Vector();
    private final String dClassName = this.getClass().getName();

    private final String dDescription = "This MBean acts as a management facade for org.apache.log4j.Hierarchy.";

    private final NotificationBroadcasterSupport nbs = new NotificationBroadcasterSupport();

    private final LoggerRepository hierarchy;

    public HierarchyDynamicMBean() {
        hierarchy = LogManager.getLoggerRepository();
        buildDynamicMBeanInfo();
    }

    @Override
    public void addAppenderEvent(final Category logger, final Appender appender) {
        log.debug("addAppenderEvent called: logger=" + logger.getName() + ", appender=" + appender.getName());
        final Notification n = new Notification(ADD_APPENDER + logger.getName(), this, 0);
        n.setUserData(appender);
        log.debug("sending notification.");
        nbs.sendNotification(n);
    }

    ObjectName addLoggerMBean(final Logger logger) {
        final String name = logger.getName();
        ObjectName objectName = null;
        try {
            final LoggerDynamicMBean loggerMBean = new LoggerDynamicMBean(logger);
            objectName = new ObjectName("log4j", "logger", name);

            if (!server.isRegistered(objectName)) {
                registerMBean(loggerMBean, objectName);
                final NotificationFilterSupport nfs = new NotificationFilterSupport();
                nfs.enableType(ADD_APPENDER + logger.getName());
                log.debug("---Adding logger [" + name + "] as listener.");
                nbs.addNotificationListener(loggerMBean, nfs, null);
                vAttributes.add(new MBeanAttributeInfo(
                        "logger=" + name,
                        "javax.management.ObjectName",
                        "The " + name + " logger.",
                        true,
                        true, // this makes
                        // the object
                        // clickable
                        false));
            }

        } catch (final JMException e) {
            log.error("Could not add loggerMBean for [" + name + "].", e);
        } catch (final RuntimeException e) {
            log.error("Could not add loggerMBean for [" + name + "].", e);
        }
        return objectName;
    }

    public ObjectName addLoggerMBean(final String name) {
        final Logger cat = LogManager.exists(name);

        if (cat != null) {
            return addLoggerMBean(cat);
        } else {
            return null;
        }
    }

    @Override
    public void addNotificationListener(
            final NotificationListener listener, final NotificationFilter filter, final java.lang.Object handback) {
        nbs.addNotificationListener(listener, filter, handback);
    }

    private void buildDynamicMBeanInfo() {
        final Constructor[] constructors = this.getClass().getConstructors();
        dConstructors[0] = new MBeanConstructorInfo(
                "HierarchyDynamicMBean(): Constructs a HierarchyDynamicMBean instance", constructors[0]);

        vAttributes.add(new MBeanAttributeInfo(
                THRESHOLD, "java.lang.String", "The \"threshold\" state of the hiearchy.", true, true, false));

        final MBeanParameterInfo[] params = new MBeanParameterInfo[1];
        params[0] = new MBeanParameterInfo("name", "java.lang.String", "Create a logger MBean");
        dOperations[0] = new MBeanOperationInfo(
                "addLoggerMBean",
                "addLoggerMBean(): add a loggerMBean",
                params,
                "javax.management.ObjectName",
                MBeanOperationInfo.ACTION);
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

        log.debug("Called getAttribute with [" + attributeName + "].");

        // Check for a recognized attributeName and call the corresponding getter
        if (attributeName.equals(THRESHOLD)) {
            return hierarchy.getThreshold();
        } else if (attributeName.startsWith("logger")) {
            final int k = attributeName.indexOf("%3D");
            String val = attributeName;
            if (k > 0) {
                val = attributeName.substring(0, k) + '=' + attributeName.substring(k + 3);
            }
            try {
                return new ObjectName("log4j:" + val);
            } catch (final JMException e) {
                log.error("Could not create ObjectName" + val);
            } catch (final RuntimeException e) {
                log.error("Could not create ObjectName" + val);
            }
        }

        // If attributeName has not been recognized throw an AttributeNotFoundException
        throw (new AttributeNotFoundException("Cannot find " + attributeName + " attribute in " + dClassName));
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        // cat.debug("getMBeanInfo called.");

        final MBeanAttributeInfo[] attribs = new MBeanAttributeInfo[vAttributes.size()];
        vAttributes.toArray(attribs);

        return new MBeanInfo(
                dClassName, dDescription, attribs, dConstructors, dOperations, new MBeanNotificationInfo[0]);
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return nbs.getNotificationInfo();
    }

    @Override
    public Object invoke(final String operationName, final Object params[], final String signature[])
            throws MBeanException, ReflectionException {

        if (operationName == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Operation name cannot be null"),
                    "Cannot invoke a null operation in " + dClassName);
        }
        // Check for a recognized operation name and call the corresponding operation

        if (operationName.equals("addLoggerMBean")) {
            return addLoggerMBean((String) params[0]);
        } else {
            throw new ReflectionException(
                    new NoSuchMethodException(operationName),
                    "Cannot find the operation " + operationName + " in " + dClassName);
        }
    }

    @Override
    public void postRegister(final java.lang.Boolean registrationDone) {
        log.debug("postRegister is called.");
        hierarchy.addHierarchyEventListener(this);
        final Logger root = hierarchy.getRootLogger();
        addLoggerMBean(root);
    }

    @Override
    public void removeAppenderEvent(final Category cat, final Appender appender) {
        log.debug("removeAppenderCalled: logger=" + cat.getName() + ", appender=" + appender.getName());
    }

    @Override
    public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
        nbs.removeNotificationListener(listener);
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

        if (name.equals(THRESHOLD)) {
            final Level l = OptionConverter.toLevel((String) value, hierarchy.getThreshold());
            hierarchy.setThreshold(l);
        }
    }
}
