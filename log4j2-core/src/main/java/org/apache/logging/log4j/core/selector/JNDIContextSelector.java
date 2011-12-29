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
package org.apache.logging.log4j.core.selector;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.helpers.Constants;
import org.apache.logging.log4j.core.javaee.ContextAnchor;
import org.apache.logging.log4j.status.StatusLogger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class can be used to define a
 * custom logger repository.  It makes use of the fact that in J2EE
 * environments, each web-application is guaranteed to have its own JNDI
 * context relative to the <code>java:comp/env</code> context. In EJBs, each
 * enterprise bean (albeit not each application) has its own context relative
 * to the <code>java:comp/env</code> context.  An <code>env-entry</code> in a
 * deployment descriptor provides the information to the JNDI context.  Once the
 * <code>env-entry</code> is set, a repository selector can query the JNDI
 * application context to look up the value of the entry. The logging context of
 * the web-application will depend on the value the env-entry.  The JNDI context
 *  which is looked up by this class is
 * <code>java:comp/env/log4j/context-name</code>.
 *
 * <p>Here is an example of an <code>env-entry<code>:
 * <blockquote>
 * <pre>
 * &lt;env-entry&gt;
 *   &lt;description&gt;JNDI logging context name for this app&lt;/description&gt;
 *   &lt;env-entry-name&gt;log4j/context-name&lt;/env-entry-name&gt;
 *   &lt;env-entry-value&gt;aDistinctiveLoggingContextName&lt;/env-entry-value&gt;
 *   &lt;env-entry-type&gt;java.lang.String&lt;/env-entry-type&gt;
 * &lt;/env-entry&gt;
 * </pre>
 * </blockquote>
 * </p>
 *
 * <p><em>If multiple applications use the same logging context name, then they
 * will share the same logging context.</em>
 * </p>
 *
 *<p>You can also specify the URL for this context's configuration resource.
 * This repository selector (ContextJNDISelector) will use this resource
 * to automatically configure the log4j repository.
 *</p>
 ** <blockquote>
 * <pre>
 * &lt;env-entry&gt;
 *   &lt;description&gt;URL for configuring log4j context&lt;/description&gt;
 *   &lt;env-entry-name&gt;log4j/configuration-resource&lt;/env-entry-name&gt;
 *   &lt;env-entry-value&gt;urlOfConfigrationResource&lt;/env-entry-value&gt;
 *   &lt;env-entry-type&gt;java.lang.String&lt;/env-entry-type&gt;
 * &lt;/env-entry&gt;
 * </pre>
 * </blockquote>
 *
 * <p>It usually good practice for configuration resources of distinct
 * applications to have distinct names. However, if this is not possible
 * Naming
 * </p>
 *
 */
public class JNDIContextSelector implements NamedContextSelector {

    private static LoggerContext context = new LoggerContext("Default");

    private static ConcurrentMap<String, LoggerContext> contextMap =
        new ConcurrentHashMap<String, LoggerContext>();

    private static StatusLogger logger = StatusLogger.getLogger();

    public LoggerContext getContext(String FQCN, boolean currentContext) {

        LoggerContext lc = ContextAnchor.THREAD_CONTEXT.get();
        if (lc != null) {
            return lc;
        }

        String loggingContextName = null;

        try {
            Context ctx = new InitialContext();
            loggingContextName = (String) lookup(ctx, Constants.JNDI_CONTEXT_NAME);
        } catch (NamingException ne) {
            logger.error("Unable to lookup " + Constants.JNDI_CONTEXT_NAME, ne);
        }

        return loggingContextName == null ? context : locateContext(loggingContextName, null);
    }

    public LoggerContext locateContext(String name, String configLocation) {
        if (name == null) {
            logger.error("A context name is required to locate a LoggerContext");
            return null;
        }
        if (!contextMap.containsKey(name)) {
            LoggerContext ctx = new LoggerContext(name, null, configLocation);
            contextMap.putIfAbsent(name, ctx);
        }
        return contextMap.get(name);
    }

    public void removeContext(LoggerContext context) {

        for (Map.Entry<String, LoggerContext> entry : contextMap.entrySet()) {
            if (entry.getValue().equals(context)) {
                contextMap.remove(entry.getKey());
            }
        }
    }

    public LoggerContext removeContext(String name) {
        return contextMap.remove(name);
    }

    public List<LoggerContext> getLoggerContexts() {
        List<LoggerContext> list = new ArrayList<LoggerContext>(contextMap.values());
        return Collections.unmodifiableList(list);
    }


    protected static Object lookup(Context ctx, String name) throws NamingException {
        if (ctx == null) {
            return null;
        }
        try {
            return ctx.lookup(name);
        } catch(NameNotFoundException e) {
            logger.error("Could not find name [" + name + "].");
            throw e;
        }
    }
}
