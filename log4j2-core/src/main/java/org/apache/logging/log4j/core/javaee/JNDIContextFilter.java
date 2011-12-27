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
package org.apache.logging.log4j.core.javaee;

import org.apache.logging.log4j.LogManager;
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

    public static final String CONTEXT_NAME = "context-name";
    public static final String CONFIG_LOCATION = "config-location";
    private ServletContext context;
    private boolean created = false;
    private String name;
    private NamedContextSelector selector = null;

    public void init(FilterConfig filterConfig) throws ServletException {
        context = filterConfig.getServletContext();
        name = filterConfig.getInitParameter(CONTEXT_NAME);
        String configLocn = filterConfig.getInitParameter(CONFIG_LOCATION);
        if (name == null) {
            throw new UnavailableException("A context-name attribute is required");
        }
        if (context.getAttribute(ContextListener.LOG4J_CONTEXT_ATTRIBUTE) == null) {
            LoggerContext ctx;
            LoggerContextFactory factory = LogManager.getFactory();
            if (factory instanceof Log4jContextFactory) {
                ContextSelector sel = ((Log4jContextFactory) factory).getSelector();
                if (sel instanceof NamedContextSelector) {
                    selector = (NamedContextSelector) sel;
                    ctx = selector.locateContext(name, configLocn);
                } else {
                    return;
                }
            } else {
                return;
            }
            context.setAttribute(ContextListener.LOG4J_CONTEXT_ATTRIBUTE, ctx);
            created = true;
            context.log("Created context for " + name + " using " + ctx.getClass().getClassLoader());
        }
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        LoggerContext ctx = (LoggerContext) context.getAttribute(ContextListener.LOG4J_CONTEXT_ATTRIBUTE);
        if (ctx != null) {
            ContextAnchor.threadContext.set(ctx);
            try {
                filterChain.doFilter(servletRequest, servletResponse);
            } finally {
                ContextAnchor.threadContext.remove();
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    public void destroy() {
        LoggerContext ctx = (LoggerContext) context.getAttribute(ContextListener.LOG4J_CONTEXT_ATTRIBUTE);
        if (ctx != null && created) {
            context.log("Removing context for " + name);
            context.removeAttribute(ContextListener.LOG4J_CONTEXT_ATTRIBUTE);
            if (selector != null) {
                selector.removeContext(name);
            }
            ctx.stop();
        }
    }
}
