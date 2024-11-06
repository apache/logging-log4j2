/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.jul.tolog4j.internal;

import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Dummy version of a java.util.Logger.
 */
public class NoOpLogger extends Logger {

    public NoOpLogger(final String name) {
        super(name, null);
    }

    @Override
    public void log(final LogRecord record) {}

    @Override
    public void log(final Level level, final String msg) {}

    @Override
    public void log(final Level level, final Supplier<String> msgSupplier) {}

    @Override
    public void log(final Level level, final String msg, final Object param1) {}

    @Override
    public void log(final Level level, final String msg, final Object[] params) {}

    @Override
    public void log(final Level level, final String msg, final Throwable thrown) {}

    @Override
    public void log(final Level level, final Throwable thrown, final Supplier<String> msgSupplier) {}

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg) {}

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final Supplier<String> msgSupplier) {}

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Object param1) {}

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Object[] params) {}

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Throwable thrown) {}

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final Throwable thrown,
            final Supplier<String> msgSupplier) {}

    @Override
    public void logrb(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String bundleName,
            final String msg) {}

    @Override
    public void logrb(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String bundleName,
            final String msg,
            final Object param1) {}

    @Override
    public void logrb(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String bundleName,
            final String msg,
            final Object[] params) {}

    @Override
    public void logrb(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final ResourceBundle bundle,
            final String msg,
            final Object... params) {}

    @Override
    public void logrb(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String bundleName,
            final String msg,
            final Throwable thrown) {}

    @Override
    public void logrb(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final ResourceBundle bundle,
            final String msg,
            final Throwable thrown) {}

    @Override
    public void entering(final String sourceClass, final String sourceMethod) {}

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object param1) {}

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {}

    @Override
    public void exiting(final String sourceClass, final String sourceMethod) {}

    @Override
    public void exiting(final String sourceClass, final String sourceMethod, final Object result) {}

    @Override
    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {}

    @Override
    public void severe(final String msg) {}

    @Override
    public void warning(final String msg) {}

    @Override
    public void info(final String msg) {}

    @Override
    public void config(final String msg) {}

    @Override
    public void fine(final String msg) {}

    @Override
    public void finer(final String msg) {}

    @Override
    public void finest(final String msg) {}

    @Override
    public void severe(final Supplier<String> msgSupplier) {}

    @Override
    public void warning(final Supplier<String> msgSupplier) {}

    @Override
    public void info(final Supplier<String> msgSupplier) {}

    @Override
    public void config(final Supplier<String> msgSupplier) {}

    @Override
    public void fine(final Supplier<String> msgSupplier) {}

    @Override
    public void finer(final Supplier<String> msgSupplier) {}

    @Override
    public void finest(final Supplier<String> msgSupplier) {}

    @Override
    public void setLevel(final Level newLevel) throws SecurityException {}

    @Override
    public Level getLevel() {
        return Level.OFF;
    }

    @Override
    public boolean isLoggable(final Level level) {
        return false;
    }
}
