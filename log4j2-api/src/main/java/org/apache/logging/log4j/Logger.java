/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
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
package org.apache.logging.log4j;

import org.apache.logging.log4j.message.Message;

/**
 * This is the central interface in the log4j package. Most logging
 * operations, except configuration, are done through this interface.
 * @doubt See LOG4J2-39.
 * @doubt See LOG4J2-16.
 */
public interface Logger {

  static Marker FLOW_MARKER = Marker.getMarker("FLOW");
  static Marker ENTRY_MARKER = Marker.getMarker("ENTRY", FLOW_MARKER);
  static Marker EXIT_MARKER = Marker.getMarker("EXIT", FLOW_MARKER);

  static Marker EXCEPTION_MARKER = Marker.getMarker("EXCEPTION");
  static Marker THROWING_MARKER = Marker.getMarker("THROWING", EXCEPTION_MARKER);
  static Marker CATCHING_MARKER = Marker.getMarker("CATCHING", EXCEPTION_MARKER);

  /**
   * Log entry to a method.
   */
  void entry();

  /**
   * Log entry to a method.
   * @param params The parameters to the method.
   * @doubt Use of varargs results in array creation which can be a substantial portion of no-op case.
   * LogMF/LogSF provides several overrides to avoid vararg except in edge cases. (RG) LogMF
   * and LogSF implement these in LogXF which calls logger.callAppenders. callAppenders is
   * part of the implementation and cannot be used by the API. Adding more methods here
   * and in AbstractLogger is sufficient.
   */
  void entry(Object... params);

  /**
   * Log exit from a method.
   */
  void exit();

  /**
   * Log exiting from a method with the result.
   * @param result The result being returned from the method call.
   */
  void exit(Object result);

  /**
   * Log an exception or error to be thrown.
   * @param t The Throwable.
   */
  void throwing(Throwable t);

  /**
   * Log an exception or error to be thrown.
   * @param level The logging Level.
   * @param t The Throwable.
   */
  void throwing(Level level, Throwable t);

  /**
   * Log an exception or error that has been caught.
   * @param t The Throwable.
   */
  void catching(Throwable t);

  /**
   * Log an exception or error that has been caught.
   * @param level The logging Level.
   * @param t The Throwable.
   */
  void catching(Level level, Throwable t);

  /**
   * Log a message object with the {@link org.apache.logging.log4j.Level#TRACE TRACE} level.
   *
   * @param message the message object to log.
   */
  void trace(String message);

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
  void trace(String message, Throwable t);

  /**
   * Log a message object with the {@link org.apache.logging.log4j.Level#TRACE TRACE} level.
   *
   * @param message the message object to log.
   */
  void trace(Object message);

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
  void trace(Object message, Throwable t);

  /**
   * Log a message with parameters at the <code>TRACE</code> level.
   * @param message the message to log.
   * @param params parameters to the message.
   */
  void trace(String message, Object... params);

  /**
   * Check whether this Logger is enabled for the TRACE  Level.
   *
   * @return boolean - <code>true</code> if this Logger is enabled for level
   *         TRACE, <code>false</code> otherwise.
   */
  boolean isTraceEnabled();

  /**
   * Check whether this Logger is enabled for the TRACE  Level.
   *
   * @param marker The marker data specific to this log statement.
   * @return boolean - <code>true</code> if this Logger is enabled for level
   *         TRACE, <code>false</code> otherwise.
   */
  boolean isTraceEnabled(Marker marker);

  /**
   * Log a message with the specific Marker at the TRACE level.
   *
   * @param msg the message string to be logged
   */
  void trace(Message msg);

  /**
   * Log a message with the specific Marker at the TRACE level.
   *
   * @param msg the message string to be logged
   * @param t   A Throwable or null.
   */
  void trace(Message msg, Throwable t);

  /**
   * Log a message with the specific Marker at the TRACE level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   */
  void trace(Marker marker, Message msg);

  /**
   * Log a message with the specific Marker at the TRACE level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   * @param t      A Throwable or null.
   */
  void trace(Marker marker, Message msg, Throwable t);

  /**
   * Log a message object with the {@link org.apache.logging.log4j.Level#DEBUG DEBUG} level.
   *
   * @param message the message object to log.
   */
  void debug(String message);

  /**
   * Log a message at the <code>DEBUG</code> level including the
   * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
   *
   * @param message the message to log.
   * @param t       the exception to log, including its stack trace.
   */
  void debug(String message, Throwable t);

  /**
   * Log a message object with the {@link org.apache.logging.log4j.Level#DEBUG DEBUG} level.
   *
   * @param message the message object to log.
   */
  void debug(Object message);

  /**
   * Log a message at the <code>DEBUG</code> level including the
   * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
   *
   * @param message the message to log.
   * @param t       the exception to log, including its stack trace.
   */
  void debug(Object message, Throwable t);

  /**
   * Log a message with parameters at the <code>DEBUG</code> level.
   * @param message the message to log.
   * @param params parameters to the message.
   */
  void debug(String message, Object... params);

  /**
   * Check whether this Logger is enabled for the DEBUG Level.
   *
   * @return boolean - <code>true</code> if this Logger is enabled for level
   *         DEBUG, <code>false</code> otherwise.
   */
  boolean isDebugEnabled();

  /**
   * Check whether this Logger is enabled for the DEBUG Level.
   *
   * @param marker The marker data specific to this log statement.
   * @return boolean - <code>true</code> if this Logger is enabled for level
   *         DEBUG, <code>false</code> otherwise.
   */
  boolean isDebugEnabled(Marker marker);

  /**
   * Log a message with the specific Marker at the DEBUG level.
   *
   * @param msg the message string to be logged
   */
  void debug(Message msg);

  /**
   * Log a message with the specific Marker at the DEBUG level.
   *
   * @param msg the message string to be logged
   * @param t   A Throwable or null.
   */
  void debug(Message msg, Throwable t);

  /**
   * Log a message with the specific Marker at the DEBUG level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   */
  void debug(Marker marker, Message msg);

  /**
   * Log a message with the specific Marker at the DEBUG level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   * @param t      A Throwable or null.
   */
  void debug(Marker marker, Message msg, Throwable t);

  /**
   * Log a message object with the {@link org.apache.logging.log4j.Level#INFO INFO} level.
   *
   * @param message the message object to log.
   */
  void info(String message);

  /**
   * Log a message at the <code>INFO</code> level including the
   * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
   *
   * @param message the message object to log.
   * @param t       the exception to log, including its stack trace.
   */
  void info(String message, Throwable t);

  /**
   * Log a message object with the {@link org.apache.logging.log4j.Level#INFO INFO} level.
   *
   * @param message the message object to log.
   */
  void info(Object message);

  /**
   * Log a message at the <code>INFO</code> level including the
   * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
   *
   * @param message the message object to log.
   * @param t       the exception to log, including its stack trace.
   */
  void info(Object message, Throwable t);

  /**
   * Log a message with parameters at the <code>INFO</code> level.
   * @param message the message to log.
   * @param params parameters to the message.
   * @doubt Likely to misinterpret existing log4j client code that intended to call info(Object,Throwable). Incurs array creation expense on every call.
   */
  void info(String message, Object... params);

  /**
   * Check whether this Logger is enabled for the INFO Level.
   *
   * @return boolean - <code>true</code> if this Logger is enabled for level
   *         INFO, <code>false</code> otherwise.
   */
  boolean isInfoEnabled();

  /**
   * Check whether this Logger is enabled for the INFO Level.
   *
   * @param marker The marker data specific to this log statement.
   * @return boolean - <code>true</code> if this Logger is enabled for level
   *         INFO, <code>false</code> otherwise.
   */
  boolean isInfoEnabled(Marker marker);

  /**
   * Log a message with the specific Marker at the TRACE level.
   *
   * @param msg the message string to be logged
   */
  void info(Message msg);

  /**
   * Log a message with the specific Marker at the INFO level.
   *
   * @param msg the message string to be logged
   * @param t   A Throwable or null.
   */
  void info(Message msg, Throwable t);

  /**
   * Log a message with the specific Marker at the INFO level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   */
  void info(Marker marker, Message msg);

  /**
   * Log a message with the specific Marker at the INFO level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   * @param t      A Throwable or null.
   */
  void info(Marker marker, Message msg, Throwable t);

 /**
   * Log a message object with the {@link org.apache.logging.log4j.Level#WARN WARN} level.
   *
   * @param message the message object to log.
   */
  void warn(String message);

  /**
   * Log a message at the <code>WARN</code> level including the
   * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
   *
   * @param message the message object to log.
   * @param t       the exception to log, including its stack trace.
   */
  void warn(String message, Throwable t);

 /**
   * Log a message object with the {@link org.apache.logging.log4j.Level#WARN WARN} level.
   *
   * @param message the message object to log.
   */
  void warn(Object message);

  /**
   * Log a message at the <code>WARN</code> level including the
   * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
   *
   * @param message the message object to log.
   * @param t       the exception to log, including its stack trace.
   */
  void warn(Object message, Throwable t);

  /**
   * Log a message with parameters at the <code>WARN</code> level.
   * @param message the message to log.
   * @param params parameters to the message.
   * @doubt Likely to misinterpret existing log4j client code that intended to call info(Object,Throwable). Incurs array creation expense on every call.
   */
  void warn(String message, Object... params);

  /**
   * Check whether this Logger is enabled for the WARN Level.
   *
   * @return boolean - <code>true</code> if this Logger is enabled for level
   *         WARN, <code>false</code> otherwise.
   */
  boolean isWarnEnabled();

  /**
   * Check whether this Logger is enabled for the WARN Level.
   *
   * @param marker The marker data specific to this log statement.
   * @return boolean - <code>true</code> if this Logger is enabled for level
   *         WARN, <code>false</code> otherwise.
   */
  boolean isWarnEnabled(Marker marker);

  /**
   * Log a message with the specific Marker at the WARN level.
   *
   * @param msg the message string to be logged
   */
  void warn(Message msg);

  /**
   * Log a message with the specific Marker at the WARN level.
   *
   * @param msg the message string to be logged
   * @param t   A Throwable or null.
   */
  void warn(Message msg, Throwable t);

  /**
   * Log a message with the specific Marker at the WARN level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   */
  void warn(Marker marker, Message msg);

  /**
   * Log a message with the specific Marker at the WARN level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   * @param t      A Throwable or null.
   */
  void warn(Marker marker, Message msg, Throwable t);

  /**
   * Log a message object with the {@link org.apache.logging.log4j.Level#ERROR ERROR} level.
   *
   * @param message the message object to log.
   */
  void error(String message);

  /**
   * Log a message at the <code>ERROR</code> level including the
   * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
   *
   * @param message the message object to log.
   * @param t       the exception to log, including its stack trace.
   */
  void error(String message, Throwable t);

  /**
   * Log a message object with the {@link org.apache.logging.log4j.Level#ERROR ERROR} level.
   *
   * @param message the message object to log.
   */
  void error(Object message);

  /**
   * Log a message at the <code>ERROR</code> level including the
   * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
   *
   * @param message the message object to log.
   * @param t       the exception to log, including its stack trace.
   */
  void error(Object message, Throwable t);

  /**
   * Log a message with parameters at the <code>ERROR</code> level.
   * @param message the message to log.
   * @param params parameters to the message.
   * @doubt Likely to misinterpret existing log4j client code that intended to call info(Object,Throwable). Incurs array creation expense on every call.
   */
  void error(String message, Object... params);

  /**
   * Check whether this Logger is enabled for the ERROR Level.
   *
   * @return boolean - <code>true</code> if this Logger is enabled for level
   *         ERROR, <code>false</code> otherwise.
   */
  boolean isErrorEnabled();

  /**
   * Check whether this Logger is enabled for the ERROR Level.
   *
   * @param marker The marker data specific to this log statement.
   * @return boolean - <code>true</code> if this Logger is enabled for level
   *         ERROR, <code>false</code> otherwise.
   */
  boolean isErrorEnabled(Marker marker);

  /**
   * Log a message with the specific Marker at the ERROR level.
   *
   * @param msg the message string to be logged
   */
  void error(Message msg);

  /**
   * Log a message with the specific Marker at the ERROR level.
   *
   * @param msg the message string to be logged
   * @param t   A Throwable or null.
   */
  void error(Message msg, Throwable t);

  /**
   * Log a message with the specific Marker at the ERROR level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   */
  void error(Marker marker, Message msg);

  /**
   * Log a message with the specific Marker at the ERROR level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   * @param t      A Throwable or null.
   */
  void error(Marker marker, Message msg, Throwable t);

  /**
   * Log a message object with the {@link org.apache.logging.log4j.Level#FATAL FATAL} level.
   *
   * @param message the message object to log.
   */
  void fatal(String message);

  /**
   * Log a message at the <code>FATAL</code> level including the
   * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
   *
   * @param message the message object to log.
   * @param t       the exception to log, including its stack trace.
   */
  void fatal(String message, Throwable t);

    /**
   * Log a message object with the {@link org.apache.logging.log4j.Level#FATAL FATAL} level.
   *
   * @param message the message object to log.
   */
  void fatal(Object message);

  /**
   * Log a message at the <code>FATAL</code> level including the
   * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
   *
   * @param message the message object to log.
   * @param t       the exception to log, including its stack trace.
   */
  void fatal(Object message, Throwable t);
  /**
   * Log a message with parameters at the <code>FATAL</code> level.
   * @param message the message to log.
   * @param params parameters to the message.
   * @doubt Likely to misinterpret existing log4j client code that intended to call info(Object,Throwable). Incurs array creation expense on every call.
   */
  void fatal(String message, Object... params);

  /**
   * Check whether this Logger is enabled for the FATAL Level.
   *
   * @return boolean - <code>true</code> if this Logger is enabled for level
   *         FATAL, <code>false</code> otherwise.
   */
  boolean isFatalEnabled();

  /**
   * Check whether this Logger is enabled for the FATAL Level.
   *
   * @param marker The marker data specific to this log statement.
   * @return boolean - <code>true</code> if this Logger is enabled for level
   *         FATAL, <code>false</code> otherwise.
   */
  boolean isFatalEnabled(Marker marker);

  /**
   * Log a message with the specific Marker at the FATAL level.
   *
   * @param msg the message string to be logged
   */
  void fatal(Message msg);

  /**
   * Log a message with the specific Marker at the FATAL level.
   *
   * @param msg the message string to be logged
   * @param t   A Throwable or null.
   */
  void fatal(Message msg, Throwable t);

  /**
   * Log a message with the specific Marker at the FATAL level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   */
  void fatal(Marker marker, Message msg);

  /**
   * Log a message with the specific Marker at the FATAL level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   * @param t      A Throwable or null.
   */
  void fatal(Marker marker, Message msg, Throwable t);
}
