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
package org.apache.logging.log4j.core.selector;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.naming.NamingException;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.net.JndiManager;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * This class can be used to define a custom logger repository. It makes use of the fact that in J2EE environments, each
 * web-application is guaranteed to have its own JNDI context relative to the <code>java:comp/env</code> context. In
 * EJBs, each enterprise bean (albeit not each application) has its own context relative to the
 * <code>java:comp/env</code> context. An <code>env-entry</code> in a deployment descriptor provides the information to
 * the JNDI context. Once the <code>env-entry</code> is set, a repository selector can query the JNDI application
 * context to look up the value of the entry. The logging context of the web-application will depend on the value the
 * env-entry. The JNDI context which is looked up by this class is <code>java:comp/env/log4j/context-name</code>.
 *
 * <p>For security reasons, JNDI must be enabled by setting system property <code>log4j2.enableJndiContextSelector=true</code>.</p>
 * <p>
 * Here is an example of an <code>env-entry</code>:
 * </p>
 * <blockquote>
 *
 * <pre>
 * &lt;env-entry&gt;
 *   &lt;description&gt;JNDI logging context name for this app&lt;/description&gt;
 *   &lt;env-entry-name&gt;log4j/context-name&lt;/env-entry-name&gt;
 *   &lt;env-entry-value&gt;aDistinctiveLoggingContextName&lt;/env-entry-value&gt;
 *   &lt;env-entry-type&gt;java.lang.String&lt;/env-entry-type&gt;
 * &lt;/env-entry&gt;
 * </pre>
 *
 * </blockquote>
 *
 * <p>
 * <em>If multiple applications use the same logging context name, then they
 * will share the same logging context.</em>
 * </p>
 *
 * <p>
 * You can also specify the URL for this context's configuration resource. This repository selector
 * (ContextJNDISelector) will use this resource to automatically configure the log4j repository.
 * </p>
 ** <blockquote>
 *
 * <pre>
 * &lt;env-entry&gt;
 *   &lt;description&gt;URL for configuring log4j context&lt;/description&gt;
 *   &lt;env-entry-name&gt;log4j/configuration-resource&lt;/env-entry-name&gt;
 *   &lt;env-entry-value&gt;urlOfConfigurationResource&lt;/env-entry-value&gt;
 *   &lt;env-entry-type&gt;java.lang.String&lt;/env-entry-type&gt;
 * &lt;/env-entry&gt;
 * </pre>
 *
 * </blockquote>
 *
 * <p>
 * It usually good practice for configuration resources of distinct applications to have distinct names. However, if
 * this is not possible Naming
 * </p>
 */
public class JndiContextSelector implements NamedContextSelector {

    private static final LoggerContext CONTEXT = new LoggerContext("Default");

    private static final ConcurrentMap<String, LoggerContext> CONTEXT_MAP = new ConcurrentHashMap<>();

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    public JndiContextSelector() {
        if (!JndiManager.isJndiContextSelectorEnabled()) {
            throw new IllegalStateException("JNDI must be enabled by setting log4j2.enableJndiContextSelector=true");
        }
    }

    @Override
    public void shutdown(
            final String fqcn, final ClassLoader loader, final boolean currentContext, final boolean allContexts) {
        LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
        if (ctx == null) {
            final String loggingContextName = getContextName();
            if (loggingContextName != null) {
                ctx = CONTEXT_MAP.get(loggingContextName);
            }
        }
        if (ctx != null) {
            ctx.stop(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public boolean hasContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
        if (ctx == null) {
            final String loggingContextName = getContextName();
            if (loggingContextName == null) {
                return false;
            }
            ctx = CONTEXT_MAP.get(loggingContextName);
        }
        return ctx != null && ctx.isStarted();
    }

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        return getContext(fqcn, loader, currentContext, null);
    }

    @Override
    public LoggerContext getContext(
            final String fqcn, final ClassLoader loader, final boolean currentContext, final URI configLocation) {

        final LoggerContext lc = ContextAnchor.THREAD_CONTEXT.get();
        if (lc != null) {
            return lc;
        }

        String loggingContextName = getContextName();

        return loggingContextName == null ? CONTEXT : locateContext(loggingContextName, null, configLocation);
    }

    private static String getContextName() {
        String loggingContextName = null;

        try (final JndiManager jndiManager = JndiManager.getDefaultManager()) {
            loggingContextName = jndiManager.lookup(Constants.JNDI_CONTEXT_NAME);
        } catch (final NamingException ne) {
            LOGGER.error("Unable to lookup {}", Constants.JNDI_CONTEXT_NAME, ne);
        }
        return loggingContextName;
    }

    @Override
    public LoggerContext locateContext(final String name, final Object externalContext, final URI configLocation) {
        if (name == null) {
            LOGGER.error("A context name is required to locate a LoggerContext");
            return null;
        }
        if (!CONTEXT_MAP.containsKey(name)) {
            final LoggerContext ctx = new LoggerContext(name, externalContext, configLocation);
            CONTEXT_MAP.putIfAbsent(name, ctx);
        }
        return CONTEXT_MAP.get(name);
    }

    @Override
    public void removeContext(final LoggerContext context) {

        for (final Map.Entry<String, LoggerContext> entry : CONTEXT_MAP.entrySet()) {
            if (entry.getValue().equals(context)) {
                CONTEXT_MAP.remove(entry.getKey());
            }
        }
    }

    @Override
    public boolean isClassLoaderDependent() {
        return false;
    }

    @Override
    public LoggerContext removeContext(final String name) {
        return CONTEXT_MAP.remove(name);
    }

    @Override
    public List<LoggerContext> getLoggerContexts() {
        return Collections.unmodifiableList(new ArrayList<>(CONTEXT_MAP.values()));
    }
}
