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
package org.apache.logging.log4j.core.jmx;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationFactory.ConfigurationSource;
import org.apache.logging.log4j.core.helpers.Assert;
import org.apache.logging.log4j.core.helpers.Charsets;
import org.apache.logging.log4j.core.helpers.Closer;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Implementation of the {@code LoggerContextAdminMBean} interface.
 */
public class LoggerContextAdmin extends NotificationBroadcasterSupport
        implements LoggerContextAdminMBean, PropertyChangeListener {
    private static final int PAGE = 4 * 1024;
    private static final int TEXT_BUFFER = 64 * 1024;
    private static final int BUFFER_SIZE = 2048;
    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private final AtomicLong sequenceNo = new AtomicLong();
    private final ObjectName objectName;
    private final LoggerContext loggerContext;
    private String customConfigText;

    /**
     * Constructs a new {@code LoggerContextAdmin} with the {@code Executor} to
     * be used for sending {@code Notification}s asynchronously to listeners.
     *
     * @param executor used to send notifications asynchronously
     * @param loggerContext the instrumented object
     */
    public LoggerContextAdmin(final LoggerContext loggerContext, final Executor executor) {
        super(executor, createNotificationInfo());
        this.loggerContext = Assert.isNotNull(loggerContext, "loggerContext");
        try {
            final String ctxName = Server.escape(loggerContext.getName());
            final String name = String.format(PATTERN, ctxName);
            objectName = new ObjectName(name);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        loggerContext.addPropertyChangeListener(this);
    }

    private static MBeanNotificationInfo createNotificationInfo() {
        final String[] notifTypes = new String[] {//
                NOTIF_TYPE_RECONFIGURED };
        final String name = Notification.class.getName();
        final String description = "Configuration reconfigured";
        return new MBeanNotificationInfo(notifTypes, name, description);
    }

    @Override
    public String getStatus() {
        return loggerContext.getStatus().toString();
    }

    @Override
    public String getName() {
        return loggerContext.getName();
    }

    private Configuration getConfig() {
        return loggerContext.getConfiguration();
    }

    @Override
    public String getConfigLocationURI() {
        if (loggerContext.getConfigLocation() != null) {
            return String.valueOf(loggerContext.getConfigLocation());
        }
        if (getConfigName() != null) {
            return String.valueOf(new File(getConfigName()).toURI());
        }
        return "";
    }

    @Override
    public void setConfigLocationURI(final String configLocation)
            throws URISyntaxException, IOException {
        LOGGER.debug("---------");
        LOGGER.debug("Remote request to reconfigure using location "
                + configLocation);
        final URI uri = new URI(configLocation);

        // validate the location first: invalid location will result in
        // default configuration being configured, try to avoid that...
        uri.toURL().openStream().close();

        loggerContext.setConfigLocation(uri);
        LOGGER.debug("Completed remote request to reconfigure.");
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if (!LoggerContext.PROPERTY_CONFIG.equals(evt.getPropertyName())) {
            return;
        }
        // erase custom text if new configuration was read from a location
        if (loggerContext.getConfiguration().getName() != null) {
            customConfigText = null;
        }
        final Notification notif = new Notification(NOTIF_TYPE_RECONFIGURED,
                getObjectName(), nextSeqNo(), now(), null);
        sendNotification(notif);
    }

    @Override
    public String getConfigText() throws IOException {
        return getConfigText(Charsets.UTF_8.name());
    }

    @Override
    public String getConfigText(final String charsetName) throws IOException {
        if (customConfigText != null) {
            return customConfigText;
        }
        try {
            final Charset charset = Charset.forName(charsetName);
            return readContents(new URI(getConfigLocationURI()), charset);
        } catch (final Exception ex) {
            final StringWriter sw = new StringWriter(BUFFER_SIZE);
            ex.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }
    }

    @Override
    public void setConfigText(final String configText, final String charsetName) {
        final String old = customConfigText;
        customConfigText = Assert.isNotNull(configText, "configText");
        LOGGER.debug("---------");
        LOGGER.debug("Remote request to reconfigure from config text.");

        try {
            final InputStream in = new ByteArrayInputStream(
                    configText.getBytes(charsetName));
            final ConfigurationSource source = new ConfigurationSource(in);
            final Configuration updated = ConfigurationFactory.getInstance()
                    .getConfiguration(source);
            loggerContext.start(updated);
            LOGGER.debug("Completed remote request to reconfigure from config text.");
        } catch (final Exception ex) {
            customConfigText = old;
            final String msg = "Could not reconfigure from config text";
            LOGGER.error(msg, ex);
            throw new IllegalArgumentException(msg, ex);
        }
    }

    /**
     * 
     * @param uri
     * @param charset MUST not be null
     * @return
     * @throws IOException
     */
    private String readContents(final URI uri, final Charset charset) throws IOException {
        InputStream in = null;
        Reader reader = null;
        try {
            in = uri.toURL().openStream();
            reader = new InputStreamReader(in, charset);
            final StringBuilder result = new StringBuilder(TEXT_BUFFER);
            final char[] buff = new char[PAGE];
            int count = -1;
            while ((count = reader.read(buff)) >= 0) {
                result.append(buff, 0, count);
            }
            return result.toString();
        } finally {
            Closer.closeSilent(in);
            Closer.closeSilent(reader);
        }
    }

    @Override
    public String getConfigName() {
        return getConfig().getName();
    }

    @Override
    public String getConfigClassName() {
        return getConfig().getClass().getName();
    }

    @Override
    public String getConfigFilter() {
        return String.valueOf(getConfig().getFilter());
    }

    @Override
    public String getConfigMonitorClassName() {
        return getConfig().getConfigurationMonitor().getClass().getName();
    }

    @Override
    public Map<String, String> getConfigProperties() {
        return getConfig().getProperties();
    }

    /**
     * Returns the {@code ObjectName} of this mbean.
     *
     * @return the {@code ObjectName}
     * @see LoggerContextAdminMBean#PATTERN
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    private long nextSeqNo() {
        return sequenceNo.getAndIncrement();
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
