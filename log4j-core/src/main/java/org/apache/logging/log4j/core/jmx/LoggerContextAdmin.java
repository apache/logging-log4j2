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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

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

    /**
     * Constructs a new {@code LoggerContextAdmin} with the {@code Executor} to be used for sending {@code Notification}
     * s asynchronously to listeners.
     *
     * @param executor used to send notifications asynchronously
     * @param loggerContext the instrumented object
     */
    public LoggerContextAdmin(final LoggerContext loggerContext, final Executor executor) {
        super(executor, createNotificationInfo());
        this.loggerContext = Objects.requireNonNull(loggerContext, "loggerContext");
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
        final String[] notifTypes = new String[] {NOTIF_TYPE_RECONFIGURED};
        final String name = Notification.class.getName();
        final String description = "Configuration reconfigured";
        return new MBeanNotificationInfo(notifTypes, name, description);
    }

    @Override
    public String getStatus() {
        return loggerContext.getState().toString();
    }

    @Override
    public String getName() {
        return loggerContext.getName();
    }

    private Configuration getConfig() {
        return loggerContext.getConfiguration();
    }

    @Override
    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The location of the configuration comes from a running configuration.")
    public String getConfigLocationUri() {
        if (loggerContext.getConfigLocation() != null) {
            return String.valueOf(loggerContext.getConfigLocation());
        }
        if (getConfigName() != null) {
            return String.valueOf(new File(getConfigName()).toURI());
        }
        return Strings.EMPTY;
    }

    @Override
    @SuppressFBWarnings(
            value = {"URLCONNECTION_SSRF_FD", "PATH_TRAVERSAL_IN"},
            justification = "This method should only be called by a secure JMX connection.")
    public void setConfigLocationUri(final String configLocation) throws URISyntaxException, IOException {
        if (configLocation == null || configLocation.isEmpty()) {
            throw new IllegalArgumentException("Missing configuration location");
        }
        LOGGER.debug("---------");
        LOGGER.debug("Remote request to reconfigure using location " + configLocation);
        final File configFile = new File(configLocation);
        ConfigurationSource configSource = null;
        if (configFile.exists()) {
            LOGGER.debug("Opening config file {}", configFile.getAbsolutePath());
            configSource = new ConfigurationSource(new FileInputStream(configFile), configFile);
        } else {
            final URL configURL = new URL(configLocation);
            LOGGER.debug("Opening config URL {}", configURL);
            configSource = new ConfigurationSource(configURL.openStream(), configURL);
        }
        final Configuration config = ConfigurationFactory.getInstance().getConfiguration(loggerContext, configSource);
        loggerContext.start(config);
        LOGGER.debug("Completed remote request to reconfigure.");
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if (!LoggerContext.PROPERTY_CONFIG.equals(evt.getPropertyName())) {
            return;
        }
        final Notification notif = new Notification(NOTIF_TYPE_RECONFIGURED, getObjectName(), nextSeqNo(), now(), null);
        sendNotification(notif);
    }

    @Override
    public String getConfigText() throws IOException {
        return getConfigText(StandardCharsets.UTF_8.name());
    }

    @Override
    @SuppressFBWarnings(
            value = "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
            justification = "JMX should be considered a trusted channel.")
    public String getConfigText(final String charsetName) throws IOException {
        try {
            final ConfigurationSource source = loggerContext.getConfiguration().getConfigurationSource();
            final ConfigurationSource copy = source.resetInputStream();
            final Charset charset = Charset.forName(charsetName);
            return readContents(copy.getInputStream(), charset);
        } catch (final Exception ex) {
            final StringWriter sw = new StringWriter(BUFFER_SIZE);
            ex.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }
    }

    /**
     * Returns the contents of the specified input stream as a String.
     * @param in stream to read from
     * @param charset MUST not be null
     * @return stream contents
     * @throws IOException if a problem occurred reading from the stream.
     */
    private String readContents(final InputStream in, final Charset charset) throws IOException {
        Reader reader = null;
        try {
            reader = new InputStreamReader(in, charset);
            final StringBuilder result = new StringBuilder(TEXT_BUFFER);
            final char[] buff = new char[PAGE];
            int count = -1;
            while ((count = reader.read(buff)) >= 0) {
                result.append(buff, 0, count);
            }
            return result.toString();
        } finally {
            Closer.closeSilently(in);
            Closer.closeSilently(reader);
        }
    }

    @Override
    public void setConfigText(final String configText, final String charsetName) {
        LOGGER.debug("---------");
        LOGGER.debug("Remote request to reconfigure from config text.");

        try {
            final InputStream in = new ByteArrayInputStream(configText.getBytes(charsetName));
            final ConfigurationSource source = new ConfigurationSource(in);
            final Configuration updated = ConfigurationFactory.getInstance().getConfiguration(loggerContext, source);
            loggerContext.start(updated);
            LOGGER.debug("Completed remote request to reconfigure from config text.");
        } catch (final Exception ex) {
            final String msg = "Could not reconfigure from config text";
            LOGGER.error(msg, ex);
            throw new IllegalArgumentException(msg, ex);
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
    public Map<String, String> getConfigProperties() {
        return getConfig().getProperties();
    }

    /**
     * Returns the {@code ObjectName} of this mbean.
     *
     * @return the {@code ObjectName}
     * @see LoggerContextAdminMBean#PATTERN
     */
    @Override
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
