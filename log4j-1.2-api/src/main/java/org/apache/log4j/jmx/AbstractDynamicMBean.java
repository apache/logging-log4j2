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

import java.util.Enumeration;
import java.util.Vector;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;

public abstract class AbstractDynamicMBean implements DynamicMBean, MBeanRegistration {

    /**
     * Get MBean name.
     *
     * @param appender appender, may not be null.
     * @return name.
     * @since 1.2.16
     */
    protected static String getAppenderName(final Appender appender) {
        String name = appender.getName();
        if (name == null || name.trim().length() == 0) {
            // try to get some form of a name, because null is not allowed (exception), and empty string certainly isn't
            // useful in
            // JMX..
            name = appender.toString();
        }
        return name;
    }

    String dClassName;
    MBeanServer server;

    private final Vector mbeanList = new Vector();

    /**
     * Enables the to get the values of several attributes of the Dynamic MBean.
     */
    @Override
    public AttributeList getAttributes(final String[] attributeNames) {

        // Check attributeNames is not null to avoid NullPointerException later on
        if (attributeNames == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("attributeNames[] cannot be null"),
                    "Cannot invoke a getter of " + dClassName);
        }

        final AttributeList resultList = new AttributeList();

        // if attributeNames is empty, return an empty result list
        if (attributeNames.length == 0) {
            return resultList;
        }

        // build the result attribute list
        for (final String attributeName : attributeNames) {
            try {
                final Object value = getAttribute((String) attributeName);
                resultList.add(new Attribute(attributeName, value));
            } catch (final JMException e) {
                e.printStackTrace();
            } catch (final RuntimeException e) {
                e.printStackTrace();
            }
        }
        return (resultList);
    }

    protected abstract Logger getLogger();

    @Override
    public void postDeregister() {
        getLogger().debug("postDeregister is called.");
    }

    @Override
    public void postRegister(final java.lang.Boolean registrationDone) {}

    /**
     * Performs cleanup for deregistering this MBean. Default implementation unregisters MBean instances which are
     * registered using {@link #registerMBean(Object mbean, ObjectName objectName)}.
     */
    @Override
    public void preDeregister() {
        getLogger().debug("preDeregister called.");

        final Enumeration iterator = mbeanList.elements();
        while (iterator.hasMoreElements()) {
            final ObjectName name = (ObjectName) iterator.nextElement();
            try {
                server.unregisterMBean(name);
            } catch (final InstanceNotFoundException e) {
                getLogger().warn("Missing MBean " + name.getCanonicalName());
            } catch (final MBeanRegistrationException e) {
                getLogger().warn("Failed unregistering " + name.getCanonicalName());
            }
        }
    }

    @Override
    public ObjectName preRegister(final MBeanServer server, final ObjectName name) {
        getLogger().debug("preRegister called. Server=" + server + ", name=" + name);
        this.server = server;
        return name;
    }

    /**
     * Registers MBean instance in the attached server. Must <em>NOT</em> be called before registration of this instance.
     */
    protected void registerMBean(final Object mbean, final ObjectName objectName)
            throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        server.registerMBean(mbean, objectName);
        mbeanList.add(objectName);
    }

    /**
     * Sets the values of several attributes of the Dynamic MBean, and returns the list of attributes that have been set.
     */
    @Override
    public AttributeList setAttributes(final AttributeList attributes) {

        // Check attributes is not null to avoid NullPointerException later on
        if (attributes == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("AttributeList attributes cannot be null"),
                    "Cannot invoke a setter of " + dClassName);
        }
        final AttributeList resultList = new AttributeList();

        // if attributeNames is empty, nothing more to do
        if (attributes.isEmpty()) {
            return resultList;
        }

        // for each attribute, try to set it and add to the result list if successfull
        for (final Object attribute : attributes) {
            final Attribute attr = (Attribute) attribute;
            try {
                setAttribute(attr);
                final String name = attr.getName();
                final Object value = getAttribute(name);
                resultList.add(new Attribute(name, value));
            } catch (final JMException e) {
                e.printStackTrace();
            } catch (final RuntimeException e) {
                e.printStackTrace();
            }
        }
        return (resultList);
    }
}
