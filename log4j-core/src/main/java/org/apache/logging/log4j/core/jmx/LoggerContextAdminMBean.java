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
import java.net.URISyntaxException;
import java.util.Map;
import javax.management.ObjectName;

/**
 * The MBean interface for monitoring and managing a {@code LoggerContext}.
 */
public interface LoggerContextAdminMBean {
    /**
     * ObjectName pattern ({@value} ) for LoggerContextAdmin MBeans. This
     * pattern contains a variable, which is the name of the logger context.
     * <p>
     * You can find all registered LoggerContextAdmin MBeans like this:
     * </p>
     *
     * <pre>
     * MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
     * String pattern = String.format(LoggerContextAdminMBean.PATTERN, &quot;*&quot;);
     * Set&lt;ObjectName&gt; loggerContextNames = mbs.queryNames(new ObjectName(pattern), null);
     * </pre>
     * <p>
     * Some characters are not allowed in ObjectNames. The logger context name
     * may be quoted. When LoggerContextAdmin MBeans are registered, their
     * ObjectNames are created using this pattern as follows:
     * </p>
     *
     * <pre>
     * String ctxName = Server.escape(loggerContext.getName());
     * String name = String.format(PATTERN, ctxName);
     * ObjectName objectName = new ObjectName(name);
     * </pre>
     *
     * @see Server#escape(String)
     */
    String PATTERN = Server.DOMAIN + ":type=%s";

    /**
     * Notification that the {@code Configuration} of the instrumented
     * {@code LoggerContext} has been reconfigured. Notifications of this type
     * ({@value} ) do not carry a message or user data.
     */
    String NOTIF_TYPE_RECONFIGURED = "com.apache.logging.log4j.core.jmx.config.reconfigured";

    /**
     * Returns the {@code ObjectName} that this MBean is registered with in the
     * MBean server.
     */
    ObjectName getObjectName();

    /**
     * Returns the status of the instrumented {@code LoggerContext}.
     *
     * @return the LoggerContext status.
     */
    String getStatus();

    /**
     * Returns the name of the instrumented {@code LoggerContext}.
     *
     * @return the name of the instrumented {@code LoggerContext}.
     */
    String getName();

    /**
     * Returns the configuration location URI as a String.
     *
     * @return the configuration location
     */
    String getConfigLocationUri();

    /**
     * Sets the configuration location to the specified URI. This will cause the
     * instrumented {@code LoggerContext} to reconfigure.
     *
     * @param configLocation location of the configuration file in
     *            {@link java.net.URI} format.
     * @throws URISyntaxException if the format of the specified
     *             configLocationURI is incorrect
     * @throws IOException if an error occurred reading the specified location
     */
    void setConfigLocationUri(String configLocation) throws URISyntaxException, IOException;

    /**
     * Returns the configuration text, which may be the contents of the
     * configuration file or the text that was last set with a call to
     * {@code setConfigText}. If reading a file, this method assumes the file's
     * character encoding is UTF-8.
     *
     * @return the configuration text
     * @throws IOException if a problem occurred reading the contents of the
     *             config file.
     */
    String getConfigText() throws IOException;

    /**
     * Returns the configuration text, which may be the contents of the
     * configuration file or the text that was last set with a call to
     * {@code setConfigText}.
     *
     * @param charsetName the encoding to use to convert the file's bytes into
     *            the resulting string.
     * @return the configuration text
     * @throws IOException if a problem occurred reading the contents of the
     *             config file.
     */
    String getConfigText(String charsetName) throws IOException;

    /**
     * Sets the configuration text. This does not replace the contents of the
     * configuration file, but <em>does</em> cause the instrumented
     * {@code LoggerContext} to be reconfigured with the specified text.
     *
     * @param configText the configuration text in XML or JSON format
     * @param charsetName name of the {@code Charset} used to convert the
     *            specified configText to bytes
     * @throws IllegalArgumentException if a problem occurs configuring from the
     *             specified text
     */
    void setConfigText(String configText, String charsetName);

    /**
     * Returns the name of the Configuration of the instrumented LoggerContext.
     *
     * @return the Configuration name
     */
    String getConfigName();

    /**
     * Returns the class name of the {@code Configuration} of the instrumented
     * LoggerContext.
     *
     * @return the class name of the {@code Configuration}.
     */
    String getConfigClassName();

    /**
     * Returns a string description of all Filters configured in the
     * {@code Configuration} of the instrumented LoggerContext.
     *
     * @return a string description of all Filters configured
     */
    String getConfigFilter();

    /**
     * Returns a map with configured properties.
     *
     * @return a map with configured properties.
     */
    Map<String, String> getConfigProperties();
}
