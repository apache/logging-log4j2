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
package org.apache.logging.log4j.migration.jcl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class written for JCL should send its logs to Log4j2 after weaving.
 *
 */
public class JclConverterExample {

    private static final LogFactory factory = LogFactory.getFactory();
    private static final Log logFromString = LogFactory.getLog("test");
    private static final Log logFromClass = LogFactory.getLog(JclConverterExample.class);

    public void testLog(Throwable t) {
        final Throwable t1 = t;
        logFromString.fatal("This is fatal.");
        logFromString.fatal("This is fatal.", t1);
        logFromString.error("This is an error.");
        logFromString.error("This is an error.", t1);
        logFromString.warn("This is a warning.");
        logFromString.warn("This is a warning.", t1);
        logFromString.info("This is informational.");
        logFromString.info("This is informational.", t1);
        logFromString.debug("This is a debug message.");
        logFromString.debug("This is a debug message.", t1);
        logFromString.trace("This is a trace message.");
        logFromString.trace("This is a trace message.", t1);
    }

    public LogFactory getLogFactory() {
        return factory;
    }

    public Log getLogFromString() {
        return logFromString;
    }

    public Log getLogFromClass() {
        return logFromClass;
    }

    public Log getLogFromFactoryAndString() {
        return factory.getInstance("test");
    }

    public Log getLogFromFactoryAndClass() {
        return factory.getInstance(JclConverterExample.class);
    }
}
