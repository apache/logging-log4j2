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

/**
 * Methods shared by LogEvent implementations.
 */
public class LocationLocator {

    static {
        System.out.println("Using Java 7 Locator");
    }


    public static StackTraceElement calcLocation(final String fqcnOfLogger) {
        if (fqcnOfLogger == null) {
            return null;
        }
        // LOG4J2-1029 new Throwable().getStackTrace is faster than Thread.currentThread().getStackTrace().
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        StackTraceElement last = null;
        for (int i = stackTrace.length - 1; i > 0; i--) {
            final String className = stackTrace[i].getClassName();
            if (fqcnOfLogger.equals(className)) {
                return last;
            }
            last = stackTrace[i];
        }
        return null;
    }

    public static StackTraceElement getStackTraceElement(final int depth) {
        return new Throwable().getStackTrace()[depth];
    }
}