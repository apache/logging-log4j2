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
package org.apache.logging.log4j.core.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.NamedContextSelector;
import org.apache.logging.log4j.spi.LoggerContextFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import java.io.IOException;

/**
 * ServletFilter than may be used to set up a LoggerContext for each web application.
 */
public class JNDIContextFilter implements Filter {
    /**
     * The Filter init parameter that defines the name of the LoggerContext.
     */
    public static final String CONTEXT_NAME = "context-name";
    /**
     * The Filter init parameter that defines the configuration location for the LoggerContext.
     */
    public static final String CONFIG_LOCATION = "config-location";
    private ServletContext context;
    private boolean created = false;
    private String name;
    private NamedContextSelector selector = null;

    public void init(final FilterConfig filterConfig) throws ServletException {
        context = filterConfig.getServletContext();
        name = filterConfig.getInitParameter(CONTEXT_NAME);
        final String configLocn = filterConfig.getInitParameter(CONFIG_LOCATION);
        if (name == null) {
            throw new UnavailableException("A context-name attribute is required");
        }
        if (context.getAttribute(Log4jContextListener.LOG4J_CONTEXT_ATTRIBUTE) == null) {
            LoggerContext ctx;
            final LoggerContextFactory factory = LogManager.getFactory();
            if (factory instanceof Log4jContextFactory) {
                final ContextSelector sel = ((Log4jContextFactory) factory).getSelector();
                if (sel instanceof NamedContextSelector) {
                    selector = (NamedContextSelector) sel;
                    ctx = selector.locateContext(name, configLocn);
                } else {
                    return;
                }
            } else {
                return;
            }
            context.setAttribute(Log4jContextListener.LOG4J_CONTEXT_ATTRIBUTE, ctx);
            created = true;
            context.log("Created context for " + name + " using " + ctx.getClass().getClassLoader());
        }
    }

    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain filterChain)
        throws IOException, ServletException {
        final LoggerContext ctx = (LoggerContext) context.getAttribute(Log4jContextListener.LOG4J_CONTEXT_ATTRIBUTE);
        if (ctx != null) {
            ContextAnchor.THREAD_CONTEXT.set(ctx);
            try {
                filterChain.doFilter(servletRequest, servletResponse);
            } finally {
                ContextAnchor.THREAD_CONTEXT.remove();
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    public void destroy() {
        final LoggerContext ctx = (LoggerContext) context.getAttribute(Log4jContextListener.LOG4J_CONTEXT_ATTRIBUTE);
        if (ctx != null && created) {
            context.log("Removing context for " + name);
            context.removeAttribute(Log4jContextListener.LOG4J_CONTEXT_ATTRIBUTE);
            if (selector != null) {
                selector.removeContext(name);
            }
            ctx.stop();
        }
    }
}
