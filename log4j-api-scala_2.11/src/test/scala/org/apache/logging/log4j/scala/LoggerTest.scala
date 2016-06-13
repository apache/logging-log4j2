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

import org.apache.logging.log4j.message.{Message, ParameterizedMessage, ParameterizedMessageFactory}
import org.apache.logging.log4j.spi.ExtendedLogger
import org.apache.logging.log4j.{Level, Marker, MarkerManager}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString, eq => eqv}
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

case class Custom(i: Int)

@RunWith(classOf[JUnitRunner])
class LoggerTest extends FunSuite with Matchers with MockitoSugar {

  val msg = new ParameterizedMessage("msg {}", 17)
  val stringMsg = "string msg"
  val cseqMsg = new StringBuilder().append("cseq msg")
  val objectMsg = Custom(17)
  val cause = new RuntimeException("cause")
  val marker = MarkerManager.getMarker("marker")

  test("fatal enabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.FATAL)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.fatal(stringMsg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.FATAL), eqv(null), any[Message], eqv(null))
  }

  test("fatal disabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.FATAL)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.fatal(stringMsg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("error enabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.ERROR)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.error(stringMsg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.ERROR), eqv(null), any[Message], eqv(null))
  }

  test("error disabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.ERROR)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.error(stringMsg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("warn enabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.WARN)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.warn(stringMsg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.WARN), eqv(null), any[Message], eqv(null))
  }

  test("warn disabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.WARN)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.warn(stringMsg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("info enabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.info(stringMsg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(null))
  }

  test("info disabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.info(stringMsg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("debug enabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.DEBUG)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.debug(stringMsg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.DEBUG), eqv(null), any[Message], eqv(null))
  }

  test("debug disabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.DEBUG)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.debug(stringMsg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("trace enabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.TRACE)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.trace(stringMsg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.TRACE), eqv(null), any[Message], eqv(null))
  }

  test("trace disabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.TRACE)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.trace(stringMsg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }


  test("log enabled with Message message and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, msg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), eqv(msg), eqv(null))
  }

  test("log disabled with Message message and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, msg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with String message and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, stringMsg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), any[Message], eqv(null))
  }

  test("log disabled with String message and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, stringMsg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with CharSequence message and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, cseqMsg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), any[Message], eqv(null))
  }

  test("log disabled with CharSequence message and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, cseqMsg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Object message and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, objectMsg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), any[Message], eqv(null))
  }

  test("log disabled with Object message and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, objectMsg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Message message and cause and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, msg, cause)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), eqv(msg), eqv(cause))
  }

  test("log disabled with Message message and cause and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, msg, cause)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with String message and cause and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, stringMsg, cause)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), any[Message], eqv(cause))
  }

  test("log disabled with String message and cause and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, stringMsg, cause)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with CharSequence message and cause and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, cseqMsg, cause)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), any[Message], eqv(cause))
  }

  test("log disabled with CharSequence message and cause and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, cseqMsg, cause)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Object message and cause and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, objectMsg, cause)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), any[Message], eqv(cause))
  }

  test("log disabled with Object message and cause and Marker") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, marker, objectMsg, cause)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Message message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, msg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), eqv(msg), eqv(null))
  }

  test("log disabled with Message message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, msg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, stringMsg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(null))
  }

  test("log disabled with String message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, stringMsg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with CharSequence message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, cseqMsg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(null))
  }

  test("log disabled with CharSequence message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, cseqMsg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Object message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, objectMsg)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(null))
  }

  test("log disabled with Object message") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, objectMsg)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Message message and cause") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, msg, cause)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), eqv(msg), eqv(cause))
  }

  test("log disabled with Message message and cause") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, msg, cause)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with String message and cause") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, stringMsg, cause)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(cause))
  }

  test("log disabled with String message and cause") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, stringMsg, cause)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with CharSequence message and cause") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, cseqMsg, cause)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(cause))
  }

  test("log disabled with CharSequence message and cause") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, cseqMsg, cause)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Object message and cause") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, objectMsg, cause)
    verify(mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(cause))
  }

  test("log disabled with Object message and cause") {
    val mockLogger = buildMockLogger
    when(mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = new Logger(mockLogger)
    logger.log(Level.INFO, objectMsg, cause)
    verify(mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }


  def buildMockLogger: ExtendedLogger = {
    val mockLogger = mock[ExtendedLogger]
    when(mockLogger.getMessageFactory).thenReturn(new ParameterizedMessageFactory)
    mockLogger
  }

}
