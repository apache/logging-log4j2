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
package org.slf4j.helpers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.slf4j.SLF4JLoggingException;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.impl.SLF4JLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class Log4JLoggerFactory implements ILoggerFactory {

    private static LoggerContext context = LogManager.getContext();

    private ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<String, Logger>();

    public static LoggerContext getContext() {
        return context;
    }

    public Logger getLogger(String name) {
        if (loggers.containsKey(name)) {
            return loggers.get(name);
        }
        org.apache.logging.log4j.Logger logger = context.getLogger(name);
        if (logger instanceof AbstractLogger) {
            loggers.putIfAbsent(name, new SLF4JLogger((AbstractLogger) logger, name));
            return loggers.get(name);
        }
        throw new SLF4JLoggingException("SLF4J Adapter requires base logging system to extend Log4J AbstractLogger");
    }
}
