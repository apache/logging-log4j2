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
package org.apache.logging.log4j.core.jmx;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Implementation of the {@code StatusLoggerAdminMBean} interface.
 */
public class StatusLoggerAdmin extends NotificationBroadcasterSupport
        implements StatusListener, StatusLoggerAdminMBean, MBeanRegistration {

    private final AtomicLong sequenceNo = new AtomicLong();
    private final ObjectName objectName;
    private final String contextName;
    private Level level = Level.WARN;

    private boolean statusListenerRegistered = false;

    private final Lock statusListenerRegistrationGuard = new ReentrantLock();

    /**
     * Constructs a new {@code StatusLoggerAdmin} with the {@code Executor} to
     * be used for sending {@code Notification}s asynchronously to listeners.
     *
     * @param contextName name of the LoggerContext under which to register this
     *            StatusLoggerAdmin. Note that the StatusLogger may be
     *            registered multiple times, once for each LoggerContext. In web
     *            containers, each web application has its own LoggerContext and
     *            by associating the StatusLogger with the LoggerContext, all
     *            associated MBeans can be unloaded when the web application is
     *            undeployed.
     * @param executor used to send notifications asynchronously
     */
    public StatusLoggerAdmin(final String contextName, final Executor executor) {
        super(executor, createNotificationInfo());
        this.contextName = contextName;
        try {
            final String mbeanName = String.format(PATTERN, Server.escape(contextName));
            objectName = new ObjectName(mbeanName);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static MBeanNotificationInfo createNotificationInfo() {
        final String[] notifTypes = new String[] {NOTIF_TYPE_DATA, NOTIF_TYPE_MESSAGE};
        final String name = Notification.class.getName();
        final String description = "StatusLogger has logged an event";
        return new MBeanNotificationInfo(notifTypes, name, description);
    }

    /// BEGIN: Conditional `StatusListener` registration ///////////////////////////////////////////////////////////////

    // `StatusLogger` contains a _fallback listener_ that defaults to a `StatusConsoleListener`.
    // It is used to propagate logs when no listeners are available.
    // If JMX registers itself always, unconditionally, this would render the fallback (console) listener ineffective.
    // That is, no status logs would be written to console when `StatusLoggerAdmin` is in the classpath.
    // To avoid this undesired behaviour, we register JMX status listener conditionally:
    // only when there is a party interested in these notifications.
    // `addNotificationListener()` is a good place to figure out such an interest.
    // Though `removeNotificationListener()` is not a good place to determine the lack of interest[1].
    // Hence, we remove the JMX status listener on JMX bean deregistration.
    //
    // [1] https://github.com/apache/logging-log4j2/pull/2249#discussion_r1469336334

    @Override
    public void addNotificationListener(
            final NotificationListener listener, final NotificationFilter filter, final Object handback) {
        super.addNotificationListener(listener, filter, handback);
        statusListenerRegistrationGuard.lock();
        try {
            if (!statusListenerRegistered) {
                StatusLogger.getLogger().registerListener(this);
                statusListenerRegistered = true;
            }
        } finally {
            statusListenerRegistrationGuard.unlock();
        }
    }

    @Override
    public ObjectName preRegister(final MBeanServer server, final ObjectName name) {
        return name;
    }

    @Override
    public void postRegister(final Boolean registrationDone) {}

    @Override
    public void preDeregister() {}

    @Override
    public void postDeregister() {
        statusListenerRegistrationGuard.lock();
        try {
            if (statusListenerRegistered) {
                StatusLogger.getLogger().removeListener(this);
                statusListenerRegistered = false;
            }
        } finally {
            statusListenerRegistrationGuard.unlock();
        }
    }

    /// END: Conditional `StatusListener` registration /////////////////////////////////////////////////////////////////

    @Override
    public String[] getStatusDataHistory() {
        final List<StatusData> data = getStatusData();
        final String[] result = new String[data.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = data.get(i).getFormattedStatus();
        }
        return result;
    }

    @Override
    public List<StatusData> getStatusData() {
        return StatusLogger.getLogger().getStatusData();
    }

    @Override
    public String getLevel() {
        return this.level.name();
    }

    @Override
    public Level getStatusLevel() {
        return this.level;
    }

    @Override
    public void setLevel(final String level) {
        this.level = Level.toLevel(level, Level.ERROR);
    }

    @Override
    public String getContextName() {
        return contextName;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.logging.log4j.status.StatusListener#log(org.apache.logging
     * .log4j.status.StatusData)
     */
    @Override
    public void log(final StatusData data) {
        final Notification notifMsg = new Notification(
                NOTIF_TYPE_MESSAGE, getObjectName(), nextSeqNo(), nowMillis(), data.getFormattedStatus());
        sendNotification(notifMsg);

        final Notification notifData = new Notification(NOTIF_TYPE_DATA, getObjectName(), nextSeqNo(), nowMillis());
        notifData.setUserData(data);
        sendNotification(notifData);
    }

    /**
     * Returns the {@code ObjectName} of this mbean.
     *
     * @return the {@code ObjectName}
     * @see StatusLoggerAdminMBean#PATTERN
     */
    @Override
    public ObjectName getObjectName() {
        return objectName;
    }

    private long nextSeqNo() {
        return sequenceNo.getAndIncrement();
    }

    private long nowMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public void close() throws IOException {
        // nothing to close here
    }
}
