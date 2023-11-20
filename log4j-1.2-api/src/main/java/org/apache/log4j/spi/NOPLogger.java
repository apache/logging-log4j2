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
package org.apache.log4j.spi;

import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Vector;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * No-operation implementation of Logger used by NOPLoggerRepository.
 *
 * @since 1.2.15
 */
public final class NOPLogger extends Logger {

    /**
     * Create instance of Logger.
     *
     * @param repo repository, may not be null.
     * @param name name, may not be null, use "root" for root logger.
     */
    public NOPLogger(final NOPLoggerRepository repo, final String name) {
        super(name);
        this.repository = repo;
        this.level = Level.OFF;
        this.parent = this;
    }

    /** {@inheritDoc} */
    @Override
    public void addAppender(final Appender newAppender) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void assertLog(final boolean assertion, final String msg) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void callAppenders(final LoggingEvent event) {
        // NOP
    }

    void closeNestedAppenders() {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void debug(final Object message) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void debug(final Object message, final Throwable t) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void error(final Object message) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void error(final Object message, final Throwable t) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void fatal(final Object message) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void fatal(final Object message, final Throwable t) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public Enumeration getAllAppenders() {
        return new Vector<>().elements();
    }

    /** {@inheritDoc} */
    @Override
    public Appender getAppender(final String name) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Priority getChainedPriority() {
        return getEffectiveLevel();
    }

    /** {@inheritDoc} */
    @Override
    public Level getEffectiveLevel() {
        return Level.OFF;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceBundle getResourceBundle() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void info(final Object message) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void info(final Object message, final Throwable t) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAttached(final Appender appender) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabledFor(final Priority level) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void l7dlog(final Priority priority, final String key, final Object[] params, final Throwable t) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void l7dlog(final Priority priority, final String key, final Throwable t) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void log(final Priority priority, final Object message) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void log(final Priority priority, final Object message, final Throwable t) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void log(final String callerFQCN, final Priority level, final Object message, final Throwable t) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void removeAllAppenders() {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void removeAppender(final Appender appender) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void removeAppender(final String name) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void setLevel(final Level level) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void setPriority(final Priority priority) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void setResourceBundle(final ResourceBundle bundle) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void trace(final Object message) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void trace(final Object message, final Throwable t) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void warn(final Object message) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void warn(final Object message, final Throwable t) {
        // NOP
    }
}
