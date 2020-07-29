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
package org.apache.logging.log4j.jul;

import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Dummy version of a java.util.Logger.
 */
public class NoOpLogger extends Logger {

    protected NoOpLogger(String name) {
        super(name, null);
    }

    @Override
    public void log(LogRecord record) {
    }

    @Override
    public void log(Level level, String msg) {
    }

    @Override
    public void log(Level level, Supplier<String> msgSupplier) {
    }

    @Override
    public void log(Level level, String msg, Object param1) {
    }

    @Override
    public void log(Level level, String msg, Object[] params) {
    }

    @Override
    public void log(Level level, String msg, Throwable thrown) {
    }

    @Override
    public void log(Level level, Throwable thrown, Supplier<String> msgSupplier) {
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, Supplier<String> msgSupplier) {
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object param1) {
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object[] params) {
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Throwable thrown) {
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, Throwable thrown,
            Supplier<String> msgSupplier) {
    }

    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg) {
    }

    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg,
            Object param1) {
    }

    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg,
            Object[] params) {
    }

    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, ResourceBundle bundle, String msg,
            Object... params) {
    }

    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg,
            Throwable thrown) {
    }

    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, ResourceBundle bundle, String msg,
            Throwable thrown) {
    }

    @Override
    public void entering(String sourceClass, String sourceMethod) {
    }

    @Override
    public void entering(String sourceClass, String sourceMethod, Object param1) {
    }

    @Override
    public void entering(String sourceClass, String sourceMethod, Object[] params) {
    }

    @Override
    public void exiting(String sourceClass, String sourceMethod) {
    }

    @Override
    public void exiting(String sourceClass, String sourceMethod, Object result) {
    }

    @Override
    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
    }

    @Override
    public void severe(String msg) {
    }

    @Override
    public void warning(String msg) {
    }

    @Override
    public void info(String msg) {
    }

    @Override
    public void config(String msg) {
    }

    @Override
    public void fine(String msg) {
    }

    @Override
    public void finer(String msg) {
    }

    @Override
    public void finest(String msg) {
    }

    @Override
    public void severe(Supplier<String> msgSupplier) {
    }

    @Override
    public void warning(Supplier<String> msgSupplier) {
    }

    @Override
    public void info(Supplier<String> msgSupplier) {
    }

    @Override
    public void config(Supplier<String> msgSupplier) {
    }

    @Override
    public void fine(Supplier<String> msgSupplier) {
    }

    @Override
    public void finer(Supplier<String> msgSupplier) {
    }

    @Override
    public void finest(Supplier<String> msgSupplier) {
    }

    @Override
    public void setLevel(Level newLevel) throws SecurityException {
    }

    @Override
    public Level getLevel() {
        return Level.OFF;
    }

    @Override
    public boolean isLoggable(Level level) {
        return false;
    }
}
