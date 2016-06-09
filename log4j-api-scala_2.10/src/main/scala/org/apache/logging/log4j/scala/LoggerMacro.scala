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
    logMarkerMsg(c)(c.universe.reify(Level.FATAL), marker, message)

  def fatalMarkerString(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[CharSequence]) =
    logMarkerString(c)(c.universe.reify(Level.FATAL), marker, message)

  def fatalMarkerObject(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[AnyRef]) =
    logMarkerObject(c)(c.universe.reify(Level.FATAL), marker, message)
  
  def fatalMarkerMsgThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMarkerMsgThrowable(c)(c.universe.reify(Level.FATAL), marker, message, cause)

  def fatalMarkerStringThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    logMarkerStringThrowable(c)(c.universe.reify(Level.FATAL), marker, message, cause)

  def fatalMarkerObjectThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    logMarkerObjectThrowable(c)(c.universe.reify(Level.FATAL), marker, message, cause)
  
  def fatalMsg(c: LoggerContext)(message: c.Expr[Message]) =
    logMsg(c)(c.universe.reify(Level.FATAL), message)

  def fatalString(c: LoggerContext)(message: c.Expr[CharSequence]) =
    logString(c)(c.universe.reify(Level.FATAL), message)

  def fatalObject(c: LoggerContext)(message: c.Expr[AnyRef]) =
    logObject(c)(c.universe.reify(Level.FATAL), message)

  def fatalMsgThrowable(c: LoggerContext)(message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMsgThrowable(c)(c.universe.reify(Level.FATAL), message, cause)

  def fatalStringThrowable(c: LoggerContext)(message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    logStringThrowable(c)(c.universe.reify(Level.FATAL), message, cause)

  def fatalObjectThrowable(c: LoggerContext)(message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    logObjectThrowable(c)(c.universe.reify(Level.FATAL), message, cause)


  def errorMarkerMsg(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message]) =
    logMarkerMsg(c)(c.universe.reify(Level.ERROR), marker, message)

  def errorMarkerString(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[CharSequence]) =
    logMarkerString(c)(c.universe.reify(Level.ERROR), marker, message)

  def errorMarkerObject(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[AnyRef]) =
    logMarkerObject(c)(c.universe.reify(Level.ERROR), marker, message)

  def errorMarkerMsgThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMarkerMsgThrowable(c)(c.universe.reify(Level.ERROR), marker, message, cause)

  def errorMarkerStringThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    logMarkerStringThrowable(c)(c.universe.reify(Level.ERROR), marker, message, cause)

  def errorMarkerObjectThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    logMarkerObjectThrowable(c)(c.universe.reify(Level.ERROR), marker, message, cause)

  def errorMsg(c: LoggerContext)(message: c.Expr[Message]) =
    logMsg(c)(c.universe.reify(Level.ERROR), message)

  def errorString(c: LoggerContext)(message: c.Expr[CharSequence]) =
    logString(c)(c.universe.reify(Level.ERROR), message)

  def errorObject(c: LoggerContext)(message: c.Expr[AnyRef]) =
    logObject(c)(c.universe.reify(Level.ERROR), message)

  def errorMsgThrowable(c: LoggerContext)(message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMsgThrowable(c)(c.universe.reify(Level.ERROR), message, cause)

  def errorStringThrowable(c: LoggerContext)(message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    logStringThrowable(c)(c.universe.reify(Level.ERROR), message, cause)

  def errorObjectThrowable(c: LoggerContext)(message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    logObjectThrowable(c)(c.universe.reify(Level.ERROR), message, cause)


  def warnMarkerMsg(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message]) =
    logMarkerMsg(c)(c.universe.reify(Level.WARN), marker, message)

  def warnMarkerString(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[CharSequence]) =
    logMarkerString(c)(c.universe.reify(Level.WARN), marker, message)

  def warnMarkerObject(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[AnyRef]) =
    logMarkerObject(c)(c.universe.reify(Level.WARN), marker, message)

  def warnMarkerMsgThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMarkerMsgThrowable(c)(c.universe.reify(Level.WARN), marker, message, cause)

  def warnMarkerStringThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    logMarkerStringThrowable(c)(c.universe.reify(Level.WARN), marker, message, cause)

  def warnMarkerObjectThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    logMarkerObjectThrowable(c)(c.universe.reify(Level.WARN), marker, message, cause)

  def warnMsg(c: LoggerContext)(message: c.Expr[Message]) =
    logMsg(c)(c.universe.reify(Level.WARN), message)

  def warnString(c: LoggerContext)(message: c.Expr[CharSequence]) =
    logString(c)(c.universe.reify(Level.WARN), message)

  def warnObject(c: LoggerContext)(message: c.Expr[AnyRef]) =
    logObject(c)(c.universe.reify(Level.WARN), message)

  def warnMsgThrowable(c: LoggerContext)(message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMsgThrowable(c)(c.universe.reify(Level.WARN), message, cause)

  def warnStringThrowable(c: LoggerContext)(message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    logStringThrowable(c)(c.universe.reify(Level.WARN), message, cause)

  def warnObjectThrowable(c: LoggerContext)(message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    logObjectThrowable(c)(c.universe.reify(Level.WARN), message, cause)


  def infoMarkerMsg(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message]) =
    logMarkerMsg(c)(c.universe.reify(Level.INFO), marker, message)

  def infoMarkerString(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[CharSequence]) =
    logMarkerString(c)(c.universe.reify(Level.INFO), marker, message)

  def infoMarkerObject(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[AnyRef]) =
    logMarkerObject(c)(c.universe.reify(Level.INFO), marker, message)

  def infoMarkerMsgThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMarkerMsgThrowable(c)(c.universe.reify(Level.INFO), marker, message, cause)

  def infoMarkerStringThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    logMarkerStringThrowable(c)(c.universe.reify(Level.INFO), marker, message, cause)

  def infoMarkerObjectThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    logMarkerObjectThrowable(c)(c.universe.reify(Level.INFO), marker, message, cause)

  def infoMsg(c: LoggerContext)(message: c.Expr[Message]) =
    logMsg(c)(c.universe.reify(Level.INFO), message)

  def infoString(c: LoggerContext)(message: c.Expr[CharSequence]) =
    logString(c)(c.universe.reify(Level.INFO), message)

  def infoObject(c: LoggerContext)(message: c.Expr[AnyRef]) =
    logObject(c)(c.universe.reify(Level.INFO), message)

  def infoMsgThrowable(c: LoggerContext)(message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMsgThrowable(c)(c.universe.reify(Level.INFO), message, cause)

  def infoStringThrowable(c: LoggerContext)(message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    logStringThrowable(c)(c.universe.reify(Level.INFO), message, cause)

  def infoObjectThrowable(c: LoggerContext)(message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    logObjectThrowable(c)(c.universe.reify(Level.INFO), message, cause)


  def debugMarkerMsg(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message]) =
    logMarkerMsg(c)(c.universe.reify(Level.DEBUG), marker, message)

  def debugMarkerString(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[CharSequence]) =
    logMarkerString(c)(c.universe.reify(Level.DEBUG), marker, message)

  def debugMarkerObject(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[AnyRef]) =
    logMarkerObject(c)(c.universe.reify(Level.DEBUG), marker, message)

  def debugMarkerMsgThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMarkerMsgThrowable(c)(c.universe.reify(Level.DEBUG), marker, message, cause)

  def debugMarkerStringThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    logMarkerStringThrowable(c)(c.universe.reify(Level.DEBUG), marker, message, cause)

  def debugMarkerObjectThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    logMarkerObjectThrowable(c)(c.universe.reify(Level.DEBUG), marker, message, cause)

  def debugMsg(c: LoggerContext)(message: c.Expr[Message]) =
    logMsg(c)(c.universe.reify(Level.DEBUG), message)

  def debugString(c: LoggerContext)(message: c.Expr[CharSequence]) =
    logString(c)(c.universe.reify(Level.DEBUG), message)

  def debugObject(c: LoggerContext)(message: c.Expr[AnyRef]) =
    logObject(c)(c.universe.reify(Level.DEBUG), message)

  def debugMsgThrowable(c: LoggerContext)(message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMsgThrowable(c)(c.universe.reify(Level.DEBUG), message, cause)

  def debugStringThrowable(c: LoggerContext)(message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    logStringThrowable(c)(c.universe.reify(Level.DEBUG), message, cause)

  def debugObjectThrowable(c: LoggerContext)(message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    logObjectThrowable(c)(c.universe.reify(Level.DEBUG), message, cause)

  
  def traceMarkerMsg(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message]) =
    logMarkerMsg(c)(c.universe.reify(Level.TRACE), marker, message)

  def traceMarkerString(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[CharSequence]) =
    logMarkerString(c)(c.universe.reify(Level.TRACE), marker, message)

  def traceMarkerObject(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[AnyRef]) =
    logMarkerObject(c)(c.universe.reify(Level.TRACE), marker, message)

  def traceMarkerMsgThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMarkerMsgThrowable(c)(c.universe.reify(Level.TRACE), marker, message, cause)

  def traceMarkerStringThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    logMarkerStringThrowable(c)(c.universe.reify(Level.TRACE), marker, message, cause)

  def traceMarkerObjectThrowable(c: LoggerContext)(marker: c.Expr[Marker], message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    logMarkerObjectThrowable(c)(c.universe.reify(Level.TRACE), marker, message, cause)

  def traceMsg(c: LoggerContext)(message: c.Expr[Message]) =
    logMsg(c)(c.universe.reify(Level.TRACE), message)

  def traceString(c: LoggerContext)(message: c.Expr[CharSequence]) =
    logString(c)(c.universe.reify(Level.TRACE), message)

  def traceObject(c: LoggerContext)(message: c.Expr[AnyRef]) =
    logObject(c)(c.universe.reify(Level.TRACE), message)

  def traceMsgThrowable(c: LoggerContext)(message: c.Expr[Message], cause: c.Expr[Throwable]) =
    logMsgThrowable(c)(c.universe.reify(Level.TRACE), message, cause)

  def traceStringThrowable(c: LoggerContext)(message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    logStringThrowable(c)(c.universe.reify(Level.TRACE), message, cause)

  def traceObjectThrowable(c: LoggerContext)(message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    logObjectThrowable(c)(c.universe.reify(Level.TRACE), message, cause)

  
  def logMarkerMsg(c: LoggerContext)(level: c.Expr[Level], marker: c.Expr[Marker], message: c.Expr[Message]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice, marker.splice)) {
        c.prefix.splice.logMessage(level.splice, marker.splice, message.splice, null)
      }
    )

  def logMarkerString(c: LoggerContext)(level: c.Expr[Level], marker: c.Expr[Marker], message: c.Expr[CharSequence]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice, marker.splice)) {
        c.prefix.splice.logMessage(level.splice, marker.splice, message.splice, null)
      }
    )

  def logMarkerObject(c: LoggerContext)(level: c.Expr[Level], marker: c.Expr[Marker], message: c.Expr[AnyRef]) =
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

  def logMarkerStringThrowable(c: LoggerContext)(level: c.Expr[Level], marker: c.Expr[Marker], message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice, marker.splice)) {
        c.prefix.splice.logMessage(level.splice, marker.splice, message.splice, cause.splice)
      }
    )

  def logMarkerObjectThrowable(c: LoggerContext)(level: c.Expr[Level], marker: c.Expr[Marker], message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
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

  def logString(c: LoggerContext)(level: c.Expr[Level], message: c.Expr[CharSequence]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice)) {
        c.prefix.splice.logMessage(level.splice, null, message.splice, null)
      }
    )

  def logObject(c: LoggerContext)(level: c.Expr[Level], message: c.Expr[AnyRef]) =
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
  
  def logStringThrowable(c: LoggerContext)(level: c.Expr[Level], message: c.Expr[CharSequence], cause: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice)) {
        c.prefix.splice.logMessage(level.splice, null, message.splice, cause.splice)
      }
    )

  def logObjectThrowable(c: LoggerContext)(level: c.Expr[Level], message: c.Expr[AnyRef], cause: c.Expr[Throwable]) =
    c.universe.reify(
      if (c.prefix.splice.delegate.isEnabled(level.splice)) {
        c.prefix.splice.logMessage(level.splice, null, message.splice, cause.splice)
      }
    )
  
}
