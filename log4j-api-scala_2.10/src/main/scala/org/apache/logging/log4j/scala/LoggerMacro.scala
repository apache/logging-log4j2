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

import org.apache.logging.log4j.message.Message
import org.apache.logging.log4j.{Level, Marker}

import scala.language.experimental.macros

import scala.reflect.macros.Context

private object LoggerMacro {

  type LoggerContext = Context { type PrefixType = Logger }

  def fatalMarkerMsg(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message]) =
    logMarkerMsg(c)(c.universe.reify(Level.WARN), marker, message)

  def fatalMarkerMsgThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMarkerMsgThrowable(c)(c.universe.reify(Level.FATAL), marker, message, cause)

  def fatalMarkerString(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String]) =
    logMarkerString(c)(c.universe.reify(Level.FATAL), marker, message)

  def fatalMarkerStringThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], cause: c.Expr[Throwable]) =
    logMarkerStringThrowable(c)(c.universe.reify(Level.FATAL), marker, message, cause)

  def fatalMsg(c: LoggerContext)(message: c.Expr[Message]) =
    logMsg(c)(c.universe.reify(Level.FATAL), message)

  def fatalMsgThrowable(c: LoggerContext)(message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMsgThrowable(c)(c.universe.reify(Level.FATAL), message, cause)

  def fatalString(c: LoggerContext)(message: c.Expr[String]) =
    logString(c)(c.universe.reify(Level.FATAL), message)

  def fatalStringThrowable(c: LoggerContext)(message: c.Expr[String], cause: c.Expr[Throwable]) =
    logStringThrowable(c)(c.universe.reify(Level.FATAL), message, cause)

  def errorMarkerMsg(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message]) =
    logMarkerMsg(c)(c.universe.reify(Level.ERROR), marker, message)

  def errorMarkerMsgThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMarkerMsgThrowable(c)(c.universe.reify(Level.ERROR), marker, message, cause)

  def errorMarkerString(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String]) =
    logMarkerString(c)(c.universe.reify(Level.ERROR), marker, message)

  def errorMarkerStringThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], cause: c.Expr[Throwable]) =
    logMarkerStringThrowable(c)(c.universe.reify(Level.ERROR), marker, message, cause)

  def errorMsg(c: LoggerContext)(message: c.Expr[Message]) =
    logMsg(c)(c.universe.reify(Level.ERROR), message)

  def errorMsgThrowable(c: LoggerContext)(message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMsgThrowable(c)(c.universe.reify(Level.ERROR), message, cause)

  def errorString(c: LoggerContext)(message: c.Expr[String]) =
    logString(c)(c.universe.reify(Level.ERROR), message)

  def errorStringThrowable(c: LoggerContext)(message: c.Expr[String], cause: c.Expr[Throwable]) =
    logStringThrowable(c)(c.universe.reify(Level.ERROR), message, cause)

  def warnMarkerMsg(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message]) =
    logMarkerMsg(c)(c.universe.reify(Level.WARN), marker, message)

  def warnMarkerMsgThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMarkerMsgThrowable(c)(c.universe.reify(Level.WARN), marker, message, cause)

  def warnMarkerString(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String]) =
    logMarkerString(c)(c.universe.reify(Level.WARN), marker, message)

  def warnMarkerStringThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], cause: c.Expr[Throwable]) =
    logMarkerStringThrowable(c)(c.universe.reify(Level.WARN), marker, message, cause)

  def warnMsg(c: LoggerContext)(message: c.Expr[Message]) =
    logMsg(c)(c.universe.reify(Level.WARN), message)

  def warnMsgThrowable(c: LoggerContext)(message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMsgThrowable(c)(c.universe.reify(Level.WARN), message, cause)

  def warnString(c: LoggerContext)(message: c.Expr[String]) =
    logString(c)(c.universe.reify(Level.WARN), message)

  def warnStringThrowable(c: LoggerContext)(message: c.Expr[String], cause: c.Expr[Throwable]) =
    logStringThrowable(c)(c.universe.reify(Level.WARN), message, cause)

  def infoMarkerMsg(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message]) =
    logMarkerMsg(c)(c.universe.reify(Level.INFO), marker, message)

  def infoMarkerMsgThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMarkerMsgThrowable(c)(c.universe.reify(Level.INFO), marker, message, cause)

  def infoMarkerString(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String]) =
    logMarkerString(c)(c.universe.reify(Level.INFO), marker, message)

  def infoMarkerStringThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], cause: c.Expr[Throwable]) =
    logMarkerStringThrowable(c)(c.universe.reify(Level.INFO), marker, message, cause)

  def infoMsg(c: LoggerContext)(message: c.Expr[Message]) =
    logMsg(c)(c.universe.reify(Level.INFO), message)

  def infoMsgThrowable(c: LoggerContext)(message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMsgThrowable(c)(c.universe.reify(Level.INFO), message, cause)

  def infoString(c: LoggerContext)(message: c.Expr[String]) =
    logString(c)(c.universe.reify(Level.INFO), message)

  def infoStringThrowable(c: LoggerContext)(message: c.Expr[String], cause: c.Expr[Throwable]) =
    logStringThrowable(c)(c.universe.reify(Level.INFO), message, cause)

  def debugMarkerMsg(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message]) =
    logMarkerMsg(c)(c.universe.reify(Level.DEBUG), marker, message)

  def debugMarkerMsgThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMarkerMsgThrowable(c)(c.universe.reify(Level.DEBUG), marker, message, cause)

  def debugMarkerString(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String]) =
    logMarkerString(c)(c.universe.reify(Level.DEBUG), marker, message)

  def debugMarkerStringThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], cause: c.Expr[Throwable]) =
    logMarkerStringThrowable(c)(c.universe.reify(Level.DEBUG), marker, message, cause)

  def debugMsg(c: LoggerContext)(message: c.Expr[Message]) =
    logMsg(c)(c.universe.reify(Level.DEBUG), message)

  def debugMsgThrowable(c: LoggerContext)(message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMsgThrowable(c)(c.universe.reify(Level.DEBUG), message, cause)

  def debugString(c: LoggerContext)(message: c.Expr[String]) =
    logString(c)(c.universe.reify(Level.DEBUG), message)

  def debugStringThrowable(c: LoggerContext)(message: c.Expr[String], cause: c.Expr[Throwable]) =
    logStringThrowable(c)(c.universe.reify(Level.DEBUG), message, cause)

  def traceMarkerMsg(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message]) =
    logMarkerMsg(c)(c.universe.reify(Level.TRACE), marker, message)

  def traceMarkerMsgThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMarkerMsgThrowable(c)(c.universe.reify(Level.TRACE), marker, message, cause)

  def traceMarkerString(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String]) =
    logMarkerString(c)(c.universe.reify(Level.TRACE), marker, message)

  def traceMarkerStringThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[String], cause: c.Expr[Throwable]) =
    logMarkerStringThrowable(c)(c.universe.reify(Level.TRACE), marker, message, cause)

  def traceMsg(c: LoggerContext)(message: c.Expr[Message]) =
    logMsg(c)(c.universe.reify(Level.TRACE), message)

  def traceMsgThrowable(c: LoggerContext)(message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMsgThrowable(c)(c.universe.reify(Level.TRACE), message, cause)

  def traceString(c: LoggerContext)(message: c.Expr[String]) =
    logString(c)(c.universe.reify(Level.TRACE), message)

  def traceStringThrowable(c: LoggerContext)(message: c.Expr[String], cause: c.Expr[Throwable]) =
    logStringThrowable(c)(c.universe.reify(Level.TRACE), message, cause)

  def logMarkerMsg(c: LoggerContext)(level: c.Expr[Level], marker: c.Expr[Marker], message: c.Expr[Message]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice, marker.splice)) {
        c.prefix.splice.logMessage(level.splice, marker.splice, message.splice, null)
      }
    )

  def logMarkerMsgThrowable(c: LoggerContext)(level: c.Expr[Level], marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice, marker.splice)) {
        c.prefix.splice.logMessage(level.splice, marker.splice, message.splice, cause.splice)
      }
    )

  def logMarkerString(c: LoggerContext)(level: c.Expr[Level], marker: c.Expr[Marker], message: c.Expr[String]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice, marker.splice)) {
        c.prefix.splice.logMessage(level.splice, marker.splice, message.splice, null)
      }
    )

  def logMarkerStringThrowable(c: LoggerContext)(level: c.Expr[Level], marker: c.Expr[Marker], message: c.Expr[String], cause: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice, marker.splice)) {
        c.prefix.splice.logMessage(level.splice, marker.splice, message.splice, cause.splice)
      }
    )

  def logMsg(c: LoggerContext)(level: c.Expr[Level], message: c.Expr[Message]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice)) {
        c.prefix.splice.logMessage(level.splice, null, message.splice, null)
      }
    )

  def logMsgThrowable(c: LoggerContext)(level: c.Expr[Level], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice)) {
        c.prefix.splice.logMessage(level.splice, null, message.splice, cause.splice)
      }
    )

  def logString(c: LoggerContext)(level: c.Expr[Level], message: c.Expr[String]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice)) {
        c.prefix.splice.logMessage(level.splice, null, message.splice, null)
      }
    )

  def logStringThrowable(c: LoggerContext)(level: c.Expr[Level], message: c.Expr[String], cause: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice)) {
        c.prefix.splice.logMessage(level.splice, null, message.splice, cause.splice)
      }
    )

}
