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
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;

/**
 * Base implementation of a Logger. It is highly recommended that any Logger implementation extend this class.
 *
 * @doubt See Jira LOG4J2-39.
 */
public abstract class AbstractLogger implements Logger {

    /**
     * Marker for flow tracing.
     */
    public static final Marker FLOW_MARKER = MarkerManager.getMarker("FLOW");
    /**
     * Marker for method entry tracing.
     */
    public static final Marker ENTRY_MARKER = MarkerManager.getMarker("ENTRY", FLOW_MARKER);
    /**
     * Marker for method exit tracing.
     */
    public static final Marker EXIT_MARKER = MarkerManager.getMarker("EXIT", FLOW_MARKER);
    /**
     * Marker for exception tracing.
     */
    public static final Marker EXCEPTION_MARKER = MarkerManager.getMarker("EXCEPTION");
    /**
     * Marker for throwing exceptions.
     */
    public static final Marker THROWING_MARKER = MarkerManager.getMarker("THROWING", EXCEPTION_MARKER);
    /**
     * Marker for catching exceptions.
     */
    public static final Marker CATCHING_MARKER = MarkerManager.getMarker("CATCHING", EXCEPTION_MARKER);

    protected String getFQCN() {
        return AbstractLogger.class.getName();
    }


    /**
     * Log entry to a method.
     */
    public void entry() {
        if (isEnabled(Level.TRACE, ENTRY_MARKER, (Object) null, null)) {
            log(ENTRY_MARKER, getFQCN(), Level.TRACE, new SimpleMessage(" entry"), null);
        }
    }


    /**
     * Log entry to a method.
     *
     * @param params The parameters to the method.
     */
    public void entry(Object... params) {
        if (isEnabled(Level.TRACE, ENTRY_MARKER, (Object) null, null)) {
            log(ENTRY_MARKER, getFQCN(), Level.TRACE, entryMsg(params.length, params), null);
        }
    }

    /**
     * Log exit from a method.
     */
    public void exit() {
        if (isEnabled(Level.TRACE, EXIT_MARKER, (Object) null, null)) {
            log(EXIT_MARKER, getFQCN(), Level.TRACE, exitMsg(null), null);
        }
    }

    /**
     * Log exiting from a method with the result.
     *
     * @param result The result being returned from the method call.
     */
    public void exit(Object result) {
        if (isEnabled(Level.TRACE, EXIT_MARKER, (Object) null, null)) {
            log(EXIT_MARKER, getFQCN(), Level.TRACE, exitMsg(result), null);
        }
    }

    /**
     * Log an exception or error to be thrown.
     *
     * @param t The Throwable.
     */
    public void throwing(Throwable t) {
        if (isEnabled(Level.ERROR, THROWING_MARKER, (Object) null, null)) {
            log(THROWING_MARKER, getFQCN(), Level.ERROR, new SimpleMessage("throwing"), t);
        }
    }


    /**
     * Log an exception or error to be thrown.
     *
     * @param level The logging Level.
     * @param t     The Throwable.
     */
    public void throwing(Level level, Throwable t) {
        if (isEnabled(level, THROWING_MARKER, (Object) null, null)) {
            log(THROWING_MARKER, getFQCN(), level, new SimpleMessage("throwing"), t);
        }
    }

    /**
     * Log an exception or error that has been caught.
     *
     * @param t The Throwable.
     */
    public void catching(Throwable t) {
        if (isEnabled(Level.DEBUG, THROWING_MARKER, (Object) null, null)) {
            log(THROWING_MARKER, getFQCN(), Level.DEBUG, new SimpleMessage("catching"), t);
        }
    }

    /**
     * Log an exception or error that has been caught.
     *
     * @param level The logging Level.
     * @param t     The Throwable.
     */
    public void catching(Level level, Throwable t) {
        if (isEnabled(level, THROWING_MARKER, (Object) null, null)) {
            log(THROWING_MARKER, getFQCN(), level, new SimpleMessage("catching"), t);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#TRACE TRACE} level.
     *
     * @param message the message object to log.
     */
    public void trace(String message) {
        if (isEnabled(Level.TRACE, null, message)) {
            log(null, getFQCN(), Level.TRACE, new SimpleMessage(message), null);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    public void trace(Marker marker, String message) {
        if (isEnabled(Level.TRACE, marker, message)) {
            log(marker, getFQCN(), Level.TRACE, new SimpleMessage(message), null);
        }
    }

    /**
     * Log a message at the <code>TRACE</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     * <p/>
     * <p>
     * See {@link #debug(String)} form for more detailed information.
     * </p>
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void trace(String message, Throwable t) {
        if (isEnabled(Level.TRACE, null, message, t)) {
            log(null, getFQCN(), Level.TRACE, new SimpleMessage(message), t);
        }
    }


    /**
     * Log a message at the <code>TRACE</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     * <p/>
     * <p>
     * See {@link #debug(String)} form for more detailed information.
     * </p>
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void trace(Marker marker, String message, Throwable t) {
        if (isEnabled(Level.TRACE, marker, message, t)) {
            log(marker, getFQCN(), Level.TRACE, new SimpleMessage(message), t);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#TRACE TRACE} level.
     *
     * @param message the message object to log.
     */
    public void trace(Object message) {
        if (isEnabled(Level.TRACE, null, message, null)) {
            log(null, getFQCN(), Level.TRACE, new ObjectMessage(message), null);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    public void trace(Marker marker, Object message) {
        if (isEnabled(Level.TRACE, marker, message, null)) {
            log(marker, getFQCN(), Level.TRACE, new ObjectMessage(message), null);
        }
    }

    /**
     * Log a message at the <code>TRACE</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     * <p/>
     * <p>
     * See {@link #debug(String)} form for more detailed information.
     * </p>
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void trace(Object message, Throwable t) {
        if (isEnabled(Level.TRACE, null, message, t)) {
            log(null, getFQCN(), Level.TRACE, new ObjectMessage(message), t);
        }
    }

    /**
     * Log a message at the <code>TRACE</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     * <p/>
     * <p>
     * See {@link #debug(String)} form for more detailed information.
     * </p>
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void trace(Marker marker, Object message, Throwable t) {
        if (isEnabled(Level.TRACE, marker, message, t)) {
            log(marker, getFQCN(), Level.TRACE, new ObjectMessage(message), t);
        }
    }

    /**
     * Log a message with parameters at the <code>TRACE</code> level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    public void trace(String message, Object... params) {
        if (isEnabled(Level.TRACE, null, message, params)) {
            ParameterizedMessage msg = new ParameterizedMessage(message, params);
            log(null, getFQCN(), Level.TRACE, msg, msg.getThrowable());
        }
    }

    /**
     * Log a message with parameters at the <code>TRACE</code> level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    public void trace(Marker marker, String message, Object... params) {
        if (isEnabled(Level.TRACE, marker, message, params)) {
            ParameterizedMessage msg = new ParameterizedMessage(message, params);
            log(marker, getFQCN(), Level.TRACE, msg, msg.getThrowable());
        }
    }

    /**
     * Check whether this Logger is enabled for the TRACE  Level.
     *
     * @return boolean - <code>true</code> if this Logger is enabled for level
     *         TRACE, <code>false</code> otherwise.
     */
    public boolean isTraceEnabled() {
        return isEnabled(Level.TRACE, null, (Object) null, null);
    }

    /**
     * Check whether this Logger is enabled for the TRACE  Level.
     *
     * @param marker The marker data.
     * @return boolean - <code>true</code> if this Logger is enabled for level
     *         TRACE, <code>false</code> otherwise.
     */
    public boolean isTraceEnabled(Marker marker) {
        return isEnabled(Level.TRACE, marker, (Object) null, null);
    }

    /**
     * Log a message with the specific Marker at the TRACE level.
     *
     * @param msg the message string to be logged
     */
    public void trace(Message msg) {
        if (isEnabled(Level.TRACE, null, msg, null)) {
            log(null, getFQCN(), Level.TRACE, msg, null);
        }
    }

    /**
     * Log a message with the specific Marker at the TRACE level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    public void trace(Message msg, Throwable t) {
        if (isEnabled(Level.TRACE, null, msg, t)) {
            log(null, getFQCN(), Level.TRACE, msg, t);
        }
    }

    /**
     * Log a message with the specific Marker at the TRACE level.
     *
     * @param marker the marker data specific to this log statement.
     * @param msg    the message string to be logged
     */
    public void trace(Marker marker, Message msg) {
        if (isEnabled(Level.TRACE, marker, msg, null)) {
            log(marker, getFQCN(), Level.TRACE, msg, null);
        }
    }

    /**
     * Log a message with the specific Marker at the TRACE level.
     *
     * @param marker the marker data specific to this log statement.
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    public void trace(Marker marker, Message msg, Throwable t) {
        if (isEnabled(Level.TRACE, marker, msg, t)) {
            log(marker, getFQCN(), Level.TRACE, msg, t);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    public void debug(String message) {
        if (isEnabled(Level.DEBUG, null, message)) {
            log(null, getFQCN(), Level.DEBUG, new SimpleMessage(message), null);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    public void debug(Marker marker, String message) {
        if (isEnabled(Level.DEBUG, marker, message)) {
            log(marker, getFQCN(), Level.DEBUG, new SimpleMessage(message), null);
        }
    }

    /**
     * Log a message at the <code>DEBUG</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param message the message to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void debug(String message, Throwable t) {
        if (isEnabled(Level.DEBUG, null, message, t)) {
            log(null, getFQCN(), Level.DEBUG, new SimpleMessage(message), t);
        }
    }

    /**
     * Log a message at the <code>DEBUG</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void debug(Marker marker, String message, Throwable t) {
        if (isEnabled(Level.DEBUG, marker, message, t)) {
            log(marker, getFQCN(), Level.DEBUG, new SimpleMessage(message), t);
        }
    }
    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    public void debug(Object message) {
        if (isEnabled(Level.DEBUG, null, message, null)) {
            log(null, getFQCN(), Level.DEBUG, new ObjectMessage(message), null);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    public void debug(Marker marker, Object message) {
        if (isEnabled(Level.DEBUG, marker, message, null)) {
            log(marker, getFQCN(), Level.DEBUG, new ObjectMessage(message), null);
        }
    }

    /**
     * Log a message at the <code>DEBUG</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param message the message to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void debug(Object message, Throwable t) {
        if (isEnabled(Level.DEBUG, null, message, t)) {
            log(null, getFQCN(), Level.DEBUG, new ObjectMessage(message), t);
        }
    }

    /**
     * Log a message at the <code>DEBUG</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void debug(Marker marker, Object message, Throwable t) {
        if (isEnabled(Level.DEBUG, marker, message, t)) {
            log(marker, getFQCN(), Level.DEBUG, new ObjectMessage(message), t);
        }
    }

    /**
     * Log a message with parameters at the <code>DEBUG</code> level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    public void debug(String message, Object... params) {
        if (isEnabled(Level.DEBUG, null, message, params)) {
            ParameterizedMessage msg = new ParameterizedMessage(message, params);
            log(null, getFQCN(), Level.DEBUG, msg, msg.getThrowable());
        }
    }

    /**
     * Log a message with parameters at the <code>DEBUG</code> level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    public void debug(Marker marker, String message, Object... params) {
        if (isEnabled(Level.DEBUG, marker, message, params)) {
            ParameterizedMessage msg = new ParameterizedMessage(message, params);
            log(marker, getFQCN(), Level.DEBUG, msg, msg.getThrowable());
        }
    }

    /**
     * Check whether this Logger is enabled for the DEBUG Level.
     *
     * @return boolean - <code>true</code> if this Logger is enabled for level
     *         DEBUG, <code>false</code> otherwise.
     */
    public boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG, null, null);
    }

    /**
     * Check whether this Logger is enabled for the DEBUG Level.
     *
     * @param marker The marker data.
     * @return boolean - <code>true</code> if this Logger is enabled for level
     *         DEBUG, <code>false</code> otherwise.
     */
    public boolean isDebugEnabled(Marker marker) {
        return isEnabled(Level.DEBUG, marker, (Object) null, null);
    }

    /**
     * Log a message with the specific Marker at the DEBUG level.
     *
     * @param msg the message string to be logged
     */
    public void debug(Message msg) {
        if (isEnabled(Level.TRACE, null, msg, null)) {
            log(null, getFQCN(), Level.TRACE, msg, null);
        }
    }

    /**
     * Log a message with the specific Marker at the DEBUG level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    public void debug(Message msg, Throwable t) {
        if (isEnabled(Level.TRACE, null, msg, t)) {
            log(null, getFQCN(), Level.TRACE, msg, t);
        }
    }

    /**
     * Log a message with the specific Marker at the DEBUG level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     */
    public void debug(Marker marker, Message msg) {
        if (isEnabled(Level.DEBUG, marker, msg, null)) {
            log(marker, getFQCN(), Level.DEBUG, msg, null);
        }
    }

    /**
     * Log a message with the specific Marker at the DEBUG level.
     *
     * @param marker the marker data specific to this log statement.
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    public void debug(Marker marker, Message msg, Throwable t) {
        if (isEnabled(Level.DEBUG, marker, msg, t)) {
            log(marker, getFQCN(), Level.DEBUG, msg, t);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#INFO INFO} level.
     *
     * @param message the message object to log.
     */
    public void info(String message) {
        if (isEnabled(Level.INFO, null, message)) {
            log(null, getFQCN(), Level.INFO, new SimpleMessage(message), null);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    public void info(Marker marker, String message) {
        if (isEnabled(Level.INFO, marker, message)) {
            log(marker, getFQCN(), Level.INFO, new SimpleMessage(message), null);
        }
    }

    /**
     * Log a message at the <code>INFO</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void info(String message, Throwable t) {
        if (isEnabled(Level.INFO, null, message, t)) {
            log(null, getFQCN(), Level.INFO, new SimpleMessage(message), t);
        }
    }

    /**
     * Log a message at the <code>INFO</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void info(Marker marker, String message, Throwable t) {
        if (isEnabled(Level.INFO, marker, message, t)) {
            log(marker, getFQCN(), Level.INFO, new SimpleMessage(message), t);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#INFO INFO} level.
     *
     * @param message the message object to log.
     */
    public void info(Object message) {
        if (isEnabled(Level.INFO, null, message, null)) {
            log(null, getFQCN(), Level.INFO, new ObjectMessage(message), null);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    public void info(Marker marker, Object message) {
        if (isEnabled(Level.INFO, marker, message, null)) {
            log(marker, getFQCN(), Level.INFO, new ObjectMessage(message), null);
        }
    }

    /**
     * Log a message at the <code>INFO</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void info(Object message, Throwable t) {
        if (isEnabled(Level.INFO, null, message, t)) {
            log(null, getFQCN(), Level.INFO, new ObjectMessage(message), t);
        }
    }


    /**
     * Log a message at the <code>INFO</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void info(Marker marker, Object message, Throwable t) {
        if (isEnabled(Level.INFO, marker, message, t)) {
            log(marker, getFQCN(), Level.INFO, new ObjectMessage(message), t);
        }
    }

    /**
     * Log a message with parameters at the <code>INFO</code> level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    public void info(String message, Object... params) {
        if (isEnabled(Level.INFO, null, message, params)) {
            ParameterizedMessage msg = new ParameterizedMessage(message, params);
            log(null, getFQCN(), Level.INFO, msg, msg.getThrowable());
        }
    }

    /**
     * Log a message with parameters at the <code>INFO</code> level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    public void info(Marker marker, String message, Object... params) {
        if (isEnabled(Level.INFO, marker, message, params)) {
            ParameterizedMessage msg = new ParameterizedMessage(message, params);
            log(marker, getFQCN(), Level.INFO, msg, msg.getThrowable());
        }
    }

    /**
     * Check whether this Logger is enabled for the INFO Level.
     *
     * @return boolean - <code>true</code> if this Logger is enabled for level
     *         INFO, <code>false</code> otherwise.
     */
    public boolean isInfoEnabled() {
        return isEnabled(Level.INFO, null, (Object) null, null);
    }

    /**
     * Check whether this Logger is enabled for the INFO Level.
     * @param marker The marker data.
     * @return boolean - <code>true</code> if this Logger is enabled for level
     *         INFO, <code>false</code> otherwise.
     */
    public boolean isInfoEnabled(Marker marker) {
        return isEnabled(Level.INFO, marker, (Object) null, null);
    }

    /**
     * Log a message with the specific Marker at the TRACE level.
     *
     * @param msg the message string to be logged
     */
    public void info(Message msg) {
        if (isEnabled(Level.INFO, null, msg, null)) {
            log(null, getFQCN(), Level.INFO, msg, null);
        }
    }

    /**
     * Log a message with the specific Marker at the INFO level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    public void info(Message msg, Throwable t) {
        if (isEnabled(Level.INFO, null, msg, t)) {
            log(null, getFQCN(), Level.INFO, msg, t);
        }
    }

    /**
     * Log a message with the specific Marker at the INFO level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     */
    public void info(Marker marker, Message msg) {
        if (isEnabled(Level.INFO, null, msg, null)) {
            log(marker, getFQCN(), Level.INFO, msg, null);
        }
    }

    /**
     * Log a message with the specific Marker at the INFO level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    public void info(Marker marker, Message msg, Throwable t) {
        if (isEnabled(Level.INFO, marker, msg, t)) {
            log(marker, getFQCN(), Level.INFO, msg, t);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#WARN WARN} level.
     *
     * @param message the message object to log.
     */
    public void warn(String message) {
        if (isEnabled(Level.WARN, null, message)) {
            log(null, getFQCN(), Level.WARN, new SimpleMessage(message), null);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    public void warn(Marker marker, String message) {
        if (isEnabled(Level.WARN, marker, message)) {
            log(marker, getFQCN(), Level.WARN, new SimpleMessage(message), null);
        }
    }

    /**
     * Log a message at the <code>WARN</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void warn(String message, Throwable t) {
        if (isEnabled(Level.WARN, null, message, t)) {
            log(null, getFQCN(), Level.DEBUG, new SimpleMessage(message), t);
        }
    }

    /**
     * Log a message at the <code>WARN</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void warn(Marker marker, String message, Throwable t) {
        if (isEnabled(Level.WARN, marker, message, t)) {
            log(marker, getFQCN(), Level.DEBUG, new SimpleMessage(message), t);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    public void warn(Marker marker, Object message) {
        if (isEnabled(Level.WARN, marker, message, null)) {
            log(marker, getFQCN(), Level.WARN, new ObjectMessage(message), null);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#WARN WARN} level.
     *
     * @param message the message object to log.
     */
    public void warn(Object message) {
        if (isEnabled(Level.WARN, null, message, null)) {
            log(null, getFQCN(), Level.WARN, new ObjectMessage(message), null);
        }
    }

    /**
     * Log a message at the <code>WARN</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void warn(Object message, Throwable t) {
        if (isEnabled(Level.WARN, null, message, t)) {
            log(null, getFQCN(), Level.DEBUG, new ObjectMessage(message), t);
        }
    }

    /**
     * Log a message at the <code>WARN</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void warn(Marker marker, Object message, Throwable t) {
        if (isEnabled(Level.WARN, marker, message, t)) {
            log(marker, getFQCN(), Level.DEBUG, new ObjectMessage(message), t);
        }
    }

    /**
     * Log a message with parameters at the <code>WARN</code> level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    public void warn(String message, Object... params) {
        if (isEnabled(Level.WARN, null, message, params)) {
            ParameterizedMessage msg = new ParameterizedMessage(message, params);
            log(null, getFQCN(), Level.WARN, msg, msg.getThrowable());
        }
    }

    /**
     * Log a message with parameters at the <code>WARN</code> level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    public void warn(Marker marker, String message, Object... params) {
        if (isEnabled(Level.WARN, marker, message, params)) {
            ParameterizedMessage msg = new ParameterizedMessage(message, params);
            log(marker, getFQCN(), Level.WARN, msg, msg.getThrowable());
        }
    }

    /**
     * Check whether this Logger is enabled for the WARN Level.
     *
     * @return boolean - <code>true</code> if this Logger is enabled for level
     *         WARN, <code>false</code> otherwise.
     */
    public boolean isWarnEnabled() {
        return isEnabled(Level.WARN, null, (Object) null, null);
    }

    /**
     * Check whether this Logger is enabled for the WARN Level.
     *
     * @param marker The marker data.
     * @return boolean - <code>true</code> if this Logger is enabled for level
     *         WARN, <code>false</code> otherwise.
     */
    public boolean isWarnEnabled(Marker marker) {
        return isEnabled(Level.WARN, marker, (Object) null, null);
    }

    /**
     * Log a message with the specific Marker at the WARN level.
     *
     * @param msg the message string to be logged
     */
    public void warn(Message msg) {
        if (isEnabled(Level.WARN, null, msg, null)) {
            log(null, getFQCN(), Level.WARN, msg, null);
        }
    }

    /**
     * Log a message with the specific Marker at the WARN level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    public void warn(Message msg, Throwable t) {
        if (isEnabled(Level.WARN, null, msg, t)) {
            log(null, getFQCN(), Level.WARN, msg, t);
        }
    }

    /**
     * Log a message with the specific Marker at the WARN level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     */
    public void warn(Marker marker, Message msg) {
        if (isEnabled(Level.WARN, null, msg, null)) {
            log(marker, getFQCN(), Level.WARN, msg, null);
        }
    }

    /**
     * Log a message with the specific Marker at the WARN level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    public void warn(Marker marker, Message msg, Throwable t) {
        if (isEnabled(Level.WARN, marker, msg, t)) {
            log(marker, getFQCN(), Level.WARN, msg, t);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#ERROR ERROR} level.
     *
     * @param message the message object to log.
     */
    public void error(String message) {
        if (isEnabled(Level.ERROR, null, message)) {
            log(null, getFQCN(), Level.ERROR, new SimpleMessage(message), null);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    public void error(Marker marker, String message) {
        if (isEnabled(Level.ERROR, marker, message)) {
            log(marker, getFQCN(), Level.ERROR, new SimpleMessage(message), null);
        }
    }

    /**
     * Log a message at the <code>ERROR</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void error(String message, Throwable t) {
        if (isEnabled(Level.ERROR, null, message, t)) {
            log(null, getFQCN(), Level.ERROR, new SimpleMessage(message), t);
        }
    }

    /**
     * Log a message at the <code>ERROR</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void error(Marker marker, String message, Throwable t) {
        if (isEnabled(Level.ERROR, marker, message, t)) {
            log(marker, getFQCN(), Level.ERROR, new SimpleMessage(message), t);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#ERROR ERROR} level.
     *
     * @param message the message object to log.
     */
    public void error(Object message) {
        if (isEnabled(Level.ERROR, null, message, null)) {
            log(null, getFQCN(), Level.ERROR, new ObjectMessage(message), null);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    public void error(Marker marker, Object message) {
        if (isEnabled(Level.ERROR, marker, message, null)) {
            log(marker, getFQCN(), Level.ERROR, new ObjectMessage(message), null);
        }
    }

    /**
     * Log a message at the <code>ERROR</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void error(Object message, Throwable t) {
        if (isEnabled(Level.ERROR, null, message, t)) {
            log(null, getFQCN(), Level.ERROR, new ObjectMessage(message), t);
        }
    }

    /**
     * Log a message at the <code>ERROR</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void error(Marker marker, Object message, Throwable t) {
        if (isEnabled(Level.ERROR, marker, message, t)) {
            log(marker, getFQCN(), Level.ERROR, new ObjectMessage(message), t);
        }
    }

    /**
     * Log a message with parameters at the <code>ERROR</code> level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    public void error(String message, Object... params) {
        if (isEnabled(Level.ERROR, null, message, params)) {
            ParameterizedMessage msg = new ParameterizedMessage(message, params);
            log(null, getFQCN(), Level.ERROR, msg, msg.getThrowable());
        }
    }

    /**
     * Log a message with parameters at the <code>ERROR</code> level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    public void error(Marker marker, String message, Object... params) {
        if (isEnabled(Level.ERROR, marker, message, params)) {
            ParameterizedMessage msg = new ParameterizedMessage(message, params);
            log(marker, getFQCN(), Level.ERROR, msg, msg.getThrowable());
        }
    }


    /**
     * Check whether this Logger is enabled for the ERROR Level.
     *
     * @return boolean - <code>true</code> if this Logger is enabled for level
     *         ERROR, <code>false</code> otherwise.
     */
    public boolean isErrorEnabled() {
        return isEnabled(Level.ERROR, null, (Object) null, null);
    }

    /**
     * Check whether this Logger is enabled for the ERROR Level.
     *
     * @param marker The marker data.
     * @return boolean - <code>true</code> if this Logger is enabled for level
     *         ERROR, <code>false</code> otherwise.
     */
    public boolean isErrorEnabled(Marker marker) {
        return isEnabled(Level.ERROR, marker, (Object) null, null);
    }

    /**
     * Log a message with the specific Marker at the ERROR level.
     *
     * @param msg the message string to be logged
     */
    public void error(Message msg) {
        if (isEnabled(Level.ERROR, null, msg, null)) {
            log(null, getFQCN(), Level.ERROR, msg, null);
        }
    }

    /**
     * Log a message with the specific Marker at the ERROR level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    public void error(Message msg, Throwable t) {
        if (isEnabled(Level.ERROR, null, msg, t)) {
            log(null, getFQCN(), Level.ERROR, msg, t);
        }
    }

    /**
     * Log a message with the specific Marker at the ERROR level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     */
    public void error(Marker marker, Message msg) {
        if (isEnabled(Level.ERROR, null, msg, null)) {
            log(null, getFQCN(), Level.ERROR, msg, null);
        }
    }

    /**
     * Log a message with the specific Marker at the ERROR level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    public void error(Marker marker, Message msg, Throwable t) {
        if (isEnabled(Level.TRACE, marker, msg, t)) {
            log(marker, getFQCN(), Level.TRACE, msg, t);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#FATAL FATAL} level.
     *
     * @param message the message object to log.
     */
    public void fatal(String message) {
        if (isEnabled(Level.FATAL, null, message)) {
            log(null, getFQCN(), Level.FATAL, new SimpleMessage(message), null);
        }
    }


    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#FATAL FATAL} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    public void fatal(Marker marker, String message) {
        if (isEnabled(Level.FATAL, marker, message)) {
            log(marker, getFQCN(), Level.FATAL, new SimpleMessage(message), null);
        }
    }

    /**
     * Log a message at the <code>FATAL</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void fatal(String message, Throwable t) {
        if (isEnabled(Level.FATAL, null, message, t)) {
            log(null, getFQCN(), Level.FATAL, new SimpleMessage(message), t);
        }
    }

    /**
     * Log a message at the <code>FATAL</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void fatal(Marker marker, String message, Throwable t) {
        if (isEnabled(Level.FATAL, marker, message, t)) {
            log(marker, getFQCN(), Level.FATAL, new SimpleMessage(message), t);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#FATAL FATAL} level.
     *
     * @param message the message object to log.
     */
    public void fatal(Object message) {
        if (isEnabled(Level.FATAL, null, message, null)) {
            log(null, getFQCN(), Level.FATAL, new ObjectMessage(message), null);
        }
    }

    /**
     * Log a message object with the {@link org.apache.logging.log4j.Level#FATAL FATAL} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    public void fatal(Marker marker, Object message) {
        if (isEnabled(Level.FATAL, marker, message, null)) {
            log(marker, getFQCN(), Level.FATAL, new ObjectMessage(message), null);
        }
    }

    /**
     * Log a message at the <code>FATAL</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void fatal(Object message, Throwable t) {
        if (isEnabled(Level.FATAL, null, message, t)) {
            log(null, getFQCN(), Level.FATAL, new ObjectMessage(message), t);
        }
    }

    /**
     * Log a message at the <code>FATAL</code> level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public void fatal(Marker marker, Object message, Throwable t) {
        if (isEnabled(Level.FATAL, marker, message, t)) {
            log(marker, getFQCN(), Level.FATAL, new ObjectMessage(message), t);
        }
    }

    /**
     * Log a message with parameters at the <code>FATAL</code> level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    public void fatal(String message, Object... params) {
        if (isEnabled(Level.FATAL, null, message, params)) {
            ParameterizedMessage msg = new ParameterizedMessage(message, params);
            log(null, getFQCN(), Level.FATAL, msg, msg.getThrowable());
        }
    }

    /**
     * Log a message with parameters at the <code>FATAL</code> level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    public void fatal(Marker marker, String message, Object... params) {
        if (isEnabled(Level.FATAL, marker, message, params)) {
            ParameterizedMessage msg = new ParameterizedMessage(message, params);
            log(marker, getFQCN(), Level.FATAL, msg, msg.getThrowable());
        }
    }

    /**
     * Check whether this Logger is enabled for the FATAL Level.
     *
     * @return boolean - <code>true</code> if this Logger is enabled for level
     *         FATAL, <code>false</code> otherwise.
     */
    public boolean isFatalEnabled() {
        return isEnabled(Level.ERROR, null, (Object) null, null);
    }

    /**
     * Check whether this Logger is enabled for the FATAL Level.
     *
     * @param marker The marker data.
     * @return boolean - <code>true</code> if this Logger is enabled for level
     *         FATAL, <code>false</code> otherwise.
     */
    public boolean isFatalEnabled(Marker marker) {
        return isEnabled(Level.FATAL, marker, (Object) null, null);
    }

    /**
     * Log a message with the specific Marker at the FATAL level.
     *
     * @param msg the message string to be logged
     */
    public void fatal(Message msg) {
        if (isEnabled(Level.FATAL, null, msg, null)) {
            log(null, getFQCN(), Level.FATAL, msg, null);
        }
    }

    /**
     * Log a message with the specific Marker at the FATAL level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    public void fatal(Message msg, Throwable t) {
        if (isEnabled(Level.FATAL, null, msg, t)) {
            log(null, getFQCN(), Level.FATAL, msg, t);
        }
    }

    /**
     * Log a message with the specific Marker at the FATAL level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     */
    public void fatal(Marker marker, Message msg) {
        if (isEnabled(Level.FATAL, null, msg, null)) {
            log(null, getFQCN(), Level.FATAL, msg, null);
        }
    }

    /**
     * Log a message with the specific Marker at the FATAL level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    public void fatal(Marker marker, Message msg, Throwable t) {
        if (isEnabled(Level.FATAL, marker, msg, t)) {
            log(marker, getFQCN(), Level.FATAL, msg, t);
        }
    }

    /**
     * Log a message with location information.
     *
     * @param marker The Marker
     * @param fqcn   The fully qualified class name of the <b>caller</b>
     * @param level  The logging level
     * @param data   The Message.
     * @param t      A Throwable or null.
     */
    protected abstract void log(Marker marker, String fqcn, Level level, Message data, Throwable t);

    /*
     * Instead of one single method with Object... declared the following methods explicitly specify
     * parameters because they perform dramatically better than having the JVM convert them to an
     * array.
     */

    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, String data);

    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @param t A Throwable.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, String data, Throwable t);

    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @param p1 The first parameter.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, String data, Object p1);

    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @param p1 The first parameter.
     * @param p2 The second parameter.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, String data, Object p1, Object p2);

    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @param p1 The first parameter.
     * @param p2 The second parameter.
     * @param p3 The third parameter.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, String data, Object p1, Object p2, Object p3);


    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @param p1 The first parameter.
     * @param p2 The second parameter.
     * @param p3 The third parameter.
     * @param params More message parameters.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, String data, Object p1, Object p2,
                                         Object p3, Object... params);

    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @param t A Throwable.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, Object data, Throwable t);

    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The Message.
     * @param t A Throwable.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, Message data, Throwable t);

    private Message entryMsg(int count, Object... params) {
        if (count == 0) {
            return new SimpleMessage(" entry");
        }
        StringBuilder sb = new StringBuilder(" entry parms(");
        int i = 0;
        for (Object parm : params) {
            if (parm != null) {
                sb.append(parm.toString());
            } else {
                sb.append("null");
            }
            if (++i < params.length) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return new SimpleMessage(sb.toString());
    }

    private Message exitMsg(Object result) {
        if (result == null) {
            return new SimpleMessage(" exit");
        }
        return new SimpleMessage(" exit with (" + result + ")");
    }
}
