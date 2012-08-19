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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.helpers.Constants;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.spi.LoggerContextFactory;

/**
 * Factory to locate a ContextSelector and then load a LoggerContext.
 */
public class Log4jContextFactory implements LoggerContextFactory {

    private ContextSelector selector;

    private StatusLogger logger = StatusLogger.getLogger();

    /**
     * Constructor that initializes the ContextSelector.
     */
    public Log4jContextFactory() {
        String sel = System.getProperty(Constants.LOG4J_CONTEXT_SELECTOR);
        if (sel != null) {
            try {
                Class clazz = Loader.loadClass(sel);
                if (clazz != null && ContextSelector.class.isAssignableFrom(clazz)) {
                    selector = (ContextSelector) clazz.newInstance();
                    return;
                }
            } catch (Exception ex) {
                logger.error("Unable to create context " + sel, ex);
            }

        }
        selector = new ClassLoaderContextSelector();
    }

    /**
     * Return the ContextSelector.
     * @return The ContextSelector.
     */
    public ContextSelector getSelector() {
        return selector;
    }

    /**
     * Load the LoggerContext using the ContextSelector.
     * @param fqcn The fully qualified class name of the caller.
     * @param currentContext If true returns the current Context, if false returns the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @return The LoggerContext.
     */
    public LoggerContext getContext(String fqcn, boolean currentContext) {
        LoggerContext ctx = selector.getContext(fqcn, currentContext);
        if (ctx.getStatus() == LoggerContext.Status.INITIALIZED) {
            ctx.start();
        }
        return ctx;
    }
}
