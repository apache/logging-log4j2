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

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.LoggerFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.impl.Log4JLogger;

/**
 *
 */
public class Log4JLoggerFactory implements ILoggerFactory {

    private static LoggerContext ctx = new Log4JContext();

    public Logger getLogger(String s) {
        return (Logger) ctx.getLogger(s);
    }

    public static LoggerContext getContext() {
        return ctx;    
    }

    private static class Log4JContext extends LoggerContext {

        private static LoggerFactory loggerFactory = new Factory();

        @Override
        public org.apache.logging.log4j.core.Logger getLogger(String name) {
            return getLogger(loggerFactory, name);
        }

    }

    private static class Factory implements LoggerFactory {

        public org.apache.logging.log4j.core.Logger newInstance(LoggerContext ctx, String name) {
            return new Log4JLogger(ctx, name);
        }
    }
}
