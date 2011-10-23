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

import org.apache.logging.log4j.core.LoggerContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 */
public class ContextListener implements ServletContextListener {

    public static final String LOG4J_CONTEXT_ATTRIBUTE = "Log4JContext";

    public static ThreadLocal<LoggerContext> threadContext = new ThreadLocal<LoggerContext>();

    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        LoggerContext ctx = new LoggerContext(context.getServletContextName(), context);
        context.setAttribute(LOG4J_CONTEXT_ATTRIBUTE, ctx);
    }

    public void contextDestroyed(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        LoggerContext ctx = (LoggerContext) context.getAttribute(LOG4J_CONTEXT_ATTRIBUTE);
        if (ctx != null) {
            context.removeAttribute(LOG4J_CONTEXT_ATTRIBUTE);
            ctx.stop();
        }
    }
}
