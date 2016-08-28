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
package org.apache.logging.log4j.scala

import org.apache.logging.log4j.message.{EntryMessage, Message, MessageFactory2}
import org.apache.logging.log4j.spi.ExtendedLogger
import org.apache.logging.log4j.{Level, LogManager, Marker}

import scala.language.experimental.macros

/**
  * Factory for [[Logger]]s.
  *
  * The [[Logging]] trait provides a simple way to get a properly named logger into a class.
  */
object Logger {

  final val FQCN = getClass.getName

    /**
    * Create a properly named [[Logger]] for a given class.
    *
    * @param clazz the class
    */
  def apply(clazz: Class[_]): Logger = Logger(LogManager.getContext(clazz.getClassLoader, false).getLogger(clazz.getName))

  /**
    * Create a [[Logger]] wrapping the given Log4j logger.
    *
    * @param delegate the Log4j logger to wrap
    */
  def apply(delegate: ExtendedLogger): Logger = new Logger(delegate)

}

/**
  * Scala wrapper for the Log4j `Logger` interface.
  *
  * Frequently the purpose of logging is to provide information about what is happening in the system,
  * which requires including information about the objects being manipulated. In Scala, you can use
  * [[http://docs.scala-lang.org/overviews/core/string-interpolation.html string interpolation]]
  * to achieve this:
  *
  * {{{
  * logger.debug(s"Logging in user ${user.getName} with birthday ${user.calcBirthday}")
  * }}}
  *
  * Since this wrapper is implemented with macros, the String construction and method invocations
  * will only occur when debug logging is enabled.
  */
class Logger private(val delegate: ExtendedLogger) extends AnyVal {

  def fatal(marker: Marker, message: Message): Unit =
  macro LoggerMacro.fatalMarkerMsg

  def fatal(marker: Marker, message: CharSequence): Unit =
  macro LoggerMacro.fatalMarkerCseq

  def fatal(marker: Marker, message: AnyRef): Unit =
  macro LoggerMacro.fatalMarkerObject

  def fatal(marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.fatalMarkerMsgThrowable

  def fatal(marker: Marker, message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.fatalMarkerCseqThrowable

  def fatal(marker: Marker, message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.fatalMarkerObjectThrowable

  def fatal(message: Message): Unit =
  macro LoggerMacro.fatalMsg

  def fatal(message: CharSequence): Unit =
  macro LoggerMacro.fatalCseq

  def fatal(message: AnyRef): Unit =
  macro LoggerMacro.fatalObject

  def fatal(message: Message, cause: Throwable): Unit =
  macro LoggerMacro.fatalMsgThrowable

  def fatal(message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.fatalCseqThrowable

  def fatal(message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.fatalObjectThrowable


  def error(marker: Marker, message: Message): Unit =
  macro LoggerMacro.errorMarkerMsg

  def error(marker: Marker, message: CharSequence): Unit =
  macro LoggerMacro.errorMarkerCseq

  def error(marker: Marker, message: AnyRef): Unit =
  macro LoggerMacro.errorMarkerObject

  def error(marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.errorMarkerMsgThrowable

  def error(marker: Marker, message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.errorMarkerCseqThrowable

  def error(marker: Marker, message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.errorMarkerObjectThrowable

  def error(message: Message): Unit =
  macro LoggerMacro.errorMsg

  def error(message: CharSequence): Unit =
  macro LoggerMacro.errorCseq

  def error(message: AnyRef): Unit =
  macro LoggerMacro.errorObject

  def error(message: Message, cause: Throwable): Unit =
  macro LoggerMacro.errorMsgThrowable

  def error(message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.errorCseqThrowable

  def error(message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.errorObjectThrowable


  def warn(marker: Marker, message: Message): Unit =
  macro LoggerMacro.warnMarkerMsg

  def warn(marker: Marker, message: CharSequence): Unit =
  macro LoggerMacro.warnMarkerCseq

  def warn(marker: Marker, message: AnyRef): Unit =
  macro LoggerMacro.warnMarkerObject

  def warn(marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.warnMarkerMsgThrowable

  def warn(marker: Marker, message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.warnMarkerCseqThrowable

  def warn(marker: Marker, message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.warnMarkerObjectThrowable

  def warn(message: Message): Unit =
  macro LoggerMacro.warnMsg

  def warn(message: CharSequence): Unit =
  macro LoggerMacro.warnCseq

  def warn(message: AnyRef): Unit =
  macro LoggerMacro.warnObject

  def warn(message: Message, cause: Throwable): Unit =
  macro LoggerMacro.warnMsgThrowable

  def warn(message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.warnCseqThrowable

  def warn(message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.warnObjectThrowable


  def info(marker: Marker, message: Message): Unit =
  macro LoggerMacro.infoMarkerMsg

  def info(marker: Marker, message: CharSequence): Unit =
  macro LoggerMacro.infoMarkerCseq

  def info(marker: Marker, message: AnyRef): Unit =
  macro LoggerMacro.infoMarkerObject

  def info(marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.infoMarkerMsgThrowable

  def info(marker: Marker, message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.infoMarkerCseqThrowable

  def info(marker: Marker, message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.infoMarkerObjectThrowable

  def info(message: Message): Unit =
  macro LoggerMacro.infoMsg

  def info(message: CharSequence): Unit =
  macro LoggerMacro.infoCseq

  def info(message: AnyRef): Unit =
  macro LoggerMacro.infoObject

  def info(message: Message, cause: Throwable): Unit =
  macro LoggerMacro.infoMsgThrowable

  def info(message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.infoCseqThrowable

  def info(message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.infoObjectThrowable


  def debug(marker: Marker, message: Message): Unit =
  macro LoggerMacro.debugMarkerMsg

  def debug(marker: Marker, message: CharSequence): Unit =
  macro LoggerMacro.debugMarkerCseq

  def debug(marker: Marker, message: AnyRef): Unit =
  macro LoggerMacro.debugMarkerObject

  def debug(marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.debugMarkerMsgThrowable

  def debug(marker: Marker, message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.debugMarkerCseqThrowable

  def debug(marker: Marker, message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.debugMarkerObjectThrowable

  def debug(message: Message): Unit =
  macro LoggerMacro.debugMsg

  def debug(message: CharSequence): Unit =
  macro LoggerMacro.debugCseq

  def debug(message: AnyRef): Unit =
  macro LoggerMacro.debugObject

  def debug(message: Message, cause: Throwable): Unit =
  macro LoggerMacro.debugMsgThrowable

  def debug(message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.debugCseqThrowable

  def debug(message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.debugObjectThrowable


  def trace(marker: Marker, message: Message): Unit =
  macro LoggerMacro.traceMarkerMsg

  def trace(marker: Marker, message: CharSequence): Unit =
  macro LoggerMacro.traceMarkerCseq

  def trace(marker: Marker, message: AnyRef): Unit =
  macro LoggerMacro.traceMarkerObject

  def trace(marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.traceMarkerMsgThrowable

  def trace(marker: Marker, message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.traceMarkerCseqThrowable

  def trace(marker: Marker, message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.traceMarkerObjectThrowable

  def trace(message: Message): Unit =
  macro LoggerMacro.traceMsg

  def trace(message: CharSequence): Unit =
  macro LoggerMacro.traceCseq

  def trace(message: AnyRef): Unit =
  macro LoggerMacro.traceObject

  def trace(message: Message, cause: Throwable): Unit =
  macro LoggerMacro.traceMsgThrowable

  def trace(message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.traceCseqThrowable

  def trace(message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.traceObjectThrowable


  /**
    * Logs a `Message` with the specific `Marker` at the given `Level`.
    *
    * @param level   the logging level
    * @param marker  the marker data specific to this log statement
    * @param message the message to be logged
    */
  def apply(level: Level, marker: Marker, message: Message): Unit =
  macro LoggerMacro.logMarkerMsg

  /**
    * Logs a string with the specific `Marker` at the given `Level`.
    *
    * @param level   the logging level
    * @param marker  the marker data specific to this log statement
    * @param message the message to be logged
    */
  def apply(level: Level, marker: Marker, message: CharSequence): Unit =
  macro LoggerMacro.logMarkerCseq

  /**
    * Logs an object with the specific `Marker` at the given `Level`.
    *
    * @param level   the logging level
    * @param marker  the marker data specific to this log statement
    * @param message the message to be logged
    */
  def apply(level: Level, marker: Marker, message: AnyRef): Unit =
  macro LoggerMacro.logMarkerObject

  /**
    * Logs a `Message` with the specific `Marker` at the given `Level` including the stack trace
    * of the given `Throwable`.
    *
    * @param level   the logging level
    * @param marker  the marker data specific to this log statement
    * @param message the message to be logged
    * @param cause   the cause
    */
  def apply(level: Level, marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.logMarkerMsgThrowable

  /**
    * Logs a string with the specific `Marker` at the given `Level` including the stack trace
    * of the given `Throwable`.
    *
    * @param level   the logging level
    * @param marker  the marker data specific to this log statement
    * @param message the message to be logged
    * @param cause   the cause
    */
  def apply(level: Level, marker: Marker, message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.logMarkerCseqThrowable

  /**
    * Logs an object with the specific `Marker` at the given `Level` including the stack trace
    * of the given `Throwable`.
    *
    * @param level   the logging level
    * @param marker  the marker data specific to this log statement
    * @param message the message to be logged
    * @param cause   the cause
    */
  def apply(level: Level, marker: Marker, message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.logMarkerObjectThrowable

  /**
    * Logs a `Message` at the given `Level`.
    *
    * @param level   the logging level
    * @param message the message to be logged
    */
  def apply(level: Level, message: Message): Unit =
  macro LoggerMacro.logMsg

  /**
    * Logs a string at the given `Level`.
    *
    * @param level   the logging level
    * @param message the message to be logged
    */
  def apply(level: Level, message: CharSequence): Unit =
  macro LoggerMacro.logCseq

  /**
    * Logs an object at the given `Level`.
    *
    * @param level   the logging level
    * @param message the message to be logged
    */
  def apply(level: Level, message: AnyRef): Unit =
  macro LoggerMacro.logObject

  /**
    * Logs a `Message` at the given `Level` including the stack trace of the given `Throwable`.
    *
    * @param level   the logging level
    * @param message the message to be logged
    * @param cause   a `Throwable`
    */
  def apply(level: Level, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.logMsgThrowable

  /**
    * Logs a string at the given `Level` including the stack trace of the given `Throwable`.
    *
    * @param level   the logging level
    * @param message the message to be logged
    * @param cause   a `Throwable`
    */
  def apply(level: Level, message: CharSequence, cause: Throwable): Unit =
  macro LoggerMacro.logCseqThrowable

  /**
    * Logs an object at the given `Level` including the stack trace of the given `Throwable`.
    *
    * @param level   the logging level
    * @param message the message to be logged
    * @param cause   a `Throwable`
    */
  def apply(level: Level, message: AnyRef, cause: Throwable): Unit =
  macro LoggerMacro.logObjectThrowable


  /**
    * Logs entry to a method. Used when the method in question has no parameters or when the parameters should not be
    * logged.
    *
    * @return The built `EntryMessage`
    */
  def traceEntry(): EntryMessage =
  macro LoggerMacro.traceEntry

  /**
    * Logs entry to a method along with its parameters.
    *
    * {{{
    * def doSomething(foo: String, bar: Int): Unit = {
    *   logger.traceEntry(foo, bar)
    *   // do something
    * }
    * }}}
    *
    * @param params the parameters to the method.
    * @return The built `EntryMessage`
    */
  def traceEntry(params: AnyRef*): EntryMessage =
  macro LoggerMacro.traceEntryParams

  /**
    * Logs entry to a method using a `Message` to describe the parameters.
    *
    * {{{
    * def doSomething(foo: Request): Unit = {
    *   logger.traceEntry(JsonMessage(foo))
    *   // do something
    * }
    * }}}
    *
    * @param message the message
    * @return The built `EntryMessage`
    */
  def traceEntry(message: Message): EntryMessage =
  macro LoggerMacro.traceEntryMessage

  /**
    * Logs exit from a method with no result.
    */
  def traceExit(): Unit =
  macro LoggerMacro.traceExit

  /**
    * Logs exiting from a method with result.
    *
    * @param result The result being returned from the method call
    * @return `result`
    */
  def traceExit[R](result: R): R =
  macro LoggerMacro.traceExitResult[R]

  /**
    * Logs exiting from a method with no result.
    *
    * @param entryMessage the `EntryMessage` returned from one of the `traceEntry` methods
    */
  def traceExit(entryMessage: EntryMessage): Unit =
  macro LoggerMacro.traceExitEntryMessage

  /**
    * Logs exiting from a method with result.
    *
    * {{{
    * def doSomething(foo: String, bar: Int): Int = {
    *   val entryMessage = logger.traceEntry(foo, bar)
    *   // do something
    *   traceExit(entryMessage, value)
    * }
    * }}}
    *
    * @param entryMessage the `EntryMessage` returned from one of the `traceEntry` methods
    * @param result       The result being returned from the method call
    * @return `result`
    */
  def traceExit[R](entryMessage: EntryMessage, result: R): R =
  macro LoggerMacro.traceExitEntryMessageResult[R]

  /**
    * Logs exiting from a method with result. Allows custom formatting of the result.
    *
    * @param message the Message containing the formatted result
    * @param result  The result being returned from the method call.
    * @return `result`
    */
  def traceExit[R](message: Message, result: R): R =
  macro LoggerMacro.traceExitMessageResult[R]

  /**
    * Logs an exception or error to be thrown.
    *
    * {{{
    *   throw logger.throwing(myException)
    * }}}
    *
    * @param t the Throwable
    * @return `t`
    */
  def throwing[T <: Throwable](t: T): T =
  macro LoggerMacro.throwing[T]

  /**
    * Logs an exception or error to be thrown to a specific logging level.
    *
    * {{{
    *   throw logger.throwing(Level.DEBUG, myException)
    * }}}
    *
    * @param level the logging Level.
    * @param t     the Throwable
    * @return `t`
    */
  def throwing[T <: Throwable](level: Level, t: T): T =
  macro LoggerMacro.throwingLevel[T]

  /**
    * Logs an exception or error that has been caught.
    *
    * @param t the Throwable.
    */
  def catching(t: Throwable): Unit =
  macro LoggerMacro.catching

  /**
    * Logs an exception or error that has been caught to a specific logging level.
    *
    * @param level The logging Level.
    * @param t     The Throwable.
    */
  def catching(level: Level, t: Throwable): Unit =
  macro LoggerMacro.catchingLevel


  /** Always logs a message at the specified level. It is the responsibility of the caller to ensure the specified
    * level is enabled.
    *
    * Should normally not be used directly from application code, but needs to be public for access by macros.
    *
    * @param level   log level
    * @param marker  marker or `null`
    * @param message message
    * @param cause   cause or `null`
    */
  def logMessage(level: Level, marker: Marker, message: Message, cause: Throwable): Unit = {
    delegate.logMessage(Logger.FQCN, level, marker, message, cause)
  }

  /** Always logs a message at the specified level. It is the responsibility of the caller to ensure the specified
    * level is enabled.
    *
    * Should normally not be used directly from application code, but needs to be public for access by macros.
    *
    * @param level   log level
    * @param marker  marker or `null`
    * @param message message
    * @param cause   cause or `null`
    */
  def logMessage(level: Level, marker: Marker, message: CharSequence, cause: Throwable): Unit = {
    delegate.logMessage(Logger.FQCN, level, marker, delegate.getMessageFactory.asInstanceOf[MessageFactory2].newMessage(message), cause)
  }

  /** Always logs a message at the specified level. It is the responsibility of the caller to ensure the specified
    * level is enabled.
    *
    * Should normally not be used directly from application code, but needs to be public for access by macros.
    *
    * @param level   log level
    * @param marker  marker or `null`
    * @param message message
    * @param cause   cause or `null`
    */
  def logMessage(level: Level, marker: Marker, message: AnyRef, cause: Throwable): Unit = {
    delegate.logMessage(Logger.FQCN, level, marker, delegate.getMessageFactory.asInstanceOf[MessageFactory2].newMessage(message), cause)
  }

}
