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
package org.apache.log4j;

import org.apache.logging.log4j.spi.LoggerContext;

/**
 *
 */
public class LogManager {

    public static Logger getRootLogger() {
        return (Logger) org.apache.logging.log4j.LogManager.getLogger("");
    }

    public static Logger getLogger(final String name) {
        return (Logger) org.apache.logging.log4j.LogManager.getLogger(name);
    }

    public static Logger getLogger(final Class clazz) {
        return (Logger) org.apache.logging.log4j.LogManager.getLogger(clazz.getName());
    }

    public static Logger exists(String name) {
        LoggerContext ctx = org.apache.logging.log4j.LogManager.getContext();
        if (!ctx.hasLogger(name)) {
            return null;
        }
        return Logger.getLogger(name);
    }
}
