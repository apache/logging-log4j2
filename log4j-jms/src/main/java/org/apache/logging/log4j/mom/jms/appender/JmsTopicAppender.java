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
package org.apache.logging.log4j.mom.jms.appender;

import java.io.Serializable;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.mom.jms.manager.JmsTopicManager;
import org.apache.logging.log4j.core.util.Booleans;

/**
 * Appender to write to a JMS Topic.
 */
@Plugin(name = "JMSTopic", category = "Core", elementType = "appender", printObject = true)
public final class JmsTopicAppender extends AbstractAppender {

    private static final long serialVersionUID = 1L;

    private final JmsTopicManager manager;

    private JmsTopicAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
                             final JmsTopicManager manager, final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        this.manager = manager;
    }

    /**
     * Actual writing occurs here.
     * <p/>
     * @param event The LogEvent.
     */
    @Override
    public void append(final LogEvent event) {
        try {
            manager.send(getLayout().toSerializable(event));
        } catch (final Exception ex) {
            throw new AppenderLoggingException(ex);
        }
    }

    /**
     * Create a JmsTopicAppender.
     * @param name The name of the Appender.
     * @param factoryName The fully qualified class name of the InitialContextFactory.
     * @param providerURL The URL of the provider to use.
     * @param urlPkgPrefixes A colon-separated list of package prefixes for the class name of the factory class that
     * will create a URL context factory
     * @param securityPrincipalName The name of the identity of the Principal.
     * @param securityCredentials The security credentials of the Principal.
     * @param factoryBindingName The name to locate in the Context that provides the TopicConnectionFactory.
     * @param topicBindingName The name to use to locate the Topic.
     * @param userName The userid to use to create the Topic Connection.
     * @param password The password to use to create the Topic Connection.
     * @param layout The layout to use (defaults to SerializedLayout).
     * @param filter The Filter or null.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @return The JmsTopicAppender.
     */
    @PluginFactory
    public static JmsTopicAppender createAppender(
            @PluginAttribute("name") final String name,
            @PluginAttribute("factoryName") final String factoryName,
            @PluginAttribute("providerURL") final String providerURL,
            @PluginAttribute("urlPkgPrefixes") final String urlPkgPrefixes,
            @PluginAttribute("securityPrincipalName") final String securityPrincipalName,
            @PluginAttribute("securityCredentials") final String securityCredentials,
            @PluginAttribute("factoryBindingName") final String factoryBindingName,
            @PluginAttribute("topicBindingName") final String topicBindingName,
            @PluginAttribute("userName") final String userName,
            @PluginAttribute("password") final String password,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("ignoreExceptions") final String ignore) {

        if (name == null) {
            LOGGER.error("No name provided for JmsQueueAppender");
            return null;
        }
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final JmsTopicManager manager = JmsTopicManager.getJmsTopicManager(factoryName, providerURL, urlPkgPrefixes,
            securityPrincipalName, securityCredentials, factoryBindingName, topicBindingName, userName, password);
        if (manager == null) {
            return null;
        }
        if (layout == null) {
            layout = SerializedLayout.createLayout();
        }
        return new JmsTopicAppender(name, filter, layout, manager, ignoreExceptions);
    }
}
