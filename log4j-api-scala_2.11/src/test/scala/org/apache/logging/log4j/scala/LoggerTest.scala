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

import org.apache.logging.log4j.message.{DefaultFlowMessageFactory, Message, ParameterizedMessage, ParameterizedMessageFactory}
import org.apache.logging.log4j.spi.{AbstractLogger, ExtendedLogger}
import org.apache.logging.log4j.{Level, Marker, MarkerManager}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString, eq => eqv}
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.language.reflectiveCalls  // needed for Mockito mocking

case class Custom(i: Int)

trait Manager {
  def fetchValue(): Int
}

@RunWith(classOf[JUnitRunner])
class LoggerTest extends FunSuite with Matchers with MockitoSugar {

  val msg = new ParameterizedMessage("msg {}", 17)
  val entryMsg = new DefaultFlowMessageFactory().newEntryMessage(msg)
  val cseqMsg: CharSequence = new StringBuilder().append("cseq msg")
  val objectMsg = Custom(17)
  val cause = new RuntimeException("cause")
  val marker = MarkerManager.getMarker("marker")
  val result = "foo"

  def fixture =
    new {
      val mockLogger = {
        val mockLogger = mock[ExtendedLogger]
        when(mockLogger.getMessageFactory).thenReturn(new ParameterizedMessageFactory)
        mockLogger
      }
      val manager = {
        val mockManager = mock[Manager]
        when(mockManager.fetchValue()).thenReturn(4711)
        mockManager
      }
    }

  test("fatal enabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.FATAL)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.fatal(s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.FATAL), eqv(null), any[Message], eqv(null))
    verify(f.manager).fetchValue()
  }

  test("fatal disabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.FATAL)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger.fatal(s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
    verify(f.manager, never).fetchValue()
  }

  test("error enabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.ERROR)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.error(s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.ERROR), eqv(null), any[Message], eqv(null))
    verify(f.manager).fetchValue()
  }

  test("error disabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.ERROR)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger.error(s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
    verify(f.manager, never).fetchValue()
  }

  test("warn enabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.WARN)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.warn(s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.WARN), eqv(null), any[Message], eqv(null))
    verify(f.manager).fetchValue()
  }

  test("warn disabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.WARN)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger.warn(s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
    verify(f.manager, never).fetchValue()
  }

  test("info enabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.info(s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(null))
    verify(f.manager).fetchValue()
  }

  test("info disabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger.info(s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
    verify(f.manager, never).fetchValue()
  }

  test("debug enabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.DEBUG)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.debug(s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.DEBUG), eqv(null), any[Message], eqv(null))
    verify(f.manager).fetchValue()
  }

  test("debug disabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.DEBUG)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger.debug(s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
    verify(f.manager, never).fetchValue()
  }

  test("trace enabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.trace(s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.TRACE), eqv(null), any[Message], eqv(null))
    verify(f.manager).fetchValue()
  }

  test("trace disabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger.trace(s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
    verify(f.manager, never).fetchValue()
  }


  test("log enabled with Message message and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, msg)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), eqv(msg), eqv(null))
  }

  test("log disabled with Message message and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, msg)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with String message and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), any[Message], eqv(null))
    verify(f.manager).fetchValue()
  }

  test("log disabled with String message and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
    verify(f.manager, never).fetchValue()
  }

  test("log enabled with CharSequence message and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, cseqMsg)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), any[Message], eqv(null))
  }

  test("log disabled with CharSequence message and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, cseqMsg)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Object message and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, objectMsg)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), any[Message], eqv(null))
  }

  test("log disabled with Object message and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, objectMsg)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Message message and cause and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, msg, cause)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), eqv(msg), eqv(cause))
  }

  test("log disabled with Message message and cause and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, msg, cause)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with String message and cause and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, s"string msg with value: ${f.manager.fetchValue()}", cause)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), any[Message], eqv(cause))
    verify(f.manager).fetchValue()
  }

  test("log disabled with String message and cause and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, s"string msg with value: ${f.manager.fetchValue()}", cause)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
    verify(f.manager, never).fetchValue()
  }

  test("log enabled with CharSequence message and cause and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, cseqMsg, cause)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), any[Message], eqv(cause))
  }

  test("log disabled with CharSequence message and cause and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, cseqMsg, cause)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Object message and cause and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, objectMsg, cause)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(marker), any[Message], eqv(cause))
  }

  test("log disabled with Object message and cause and Marker") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO, marker)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, marker, objectMsg, cause)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Message message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, msg)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), eqv(msg), eqv(null))
  }

  test("log disabled with Message message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, msg)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(null))
    verify(f.manager).fetchValue()
  }

  test("log disabled with String message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, s"string msg with value: ${f.manager.fetchValue()}")
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
    verify(f.manager, never).fetchValue()
  }

  test("log enabled with CharSequence message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, cseqMsg)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(null))
  }

  test("log disabled with CharSequence message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, cseqMsg)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Object message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, objectMsg)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(null))
  }

  test("log disabled with Object message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, objectMsg)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Message message and cause") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, msg, cause)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), eqv(msg), eqv(cause))
  }

  test("log disabled with Message message and cause") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, msg, cause)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with String message and cause") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, s"string msg with value: ${f.manager.fetchValue()}", cause)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(cause))
    verify(f.manager).fetchValue()
  }

  test("log disabled with String message and cause") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, s"string msg with value: ${f.manager.fetchValue()}", cause)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
    verify(f.manager, never).fetchValue()
  }

  test("log enabled with CharSequence message and cause") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, cseqMsg, cause)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(cause))
  }

  test("log disabled with CharSequence message and cause") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, cseqMsg, cause)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }

  test("log enabled with Object message and cause") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, objectMsg, cause)
    verify(f.mockLogger).logMessage(anyString(), eqv(Level.INFO), eqv(null), any[Message], eqv(cause))
  }

  test("log disabled with Object message and cause") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.INFO)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger(Level.INFO, objectMsg, cause)
    verify(f.mockLogger, never).logMessage(anyString(), any[Level], any[Marker], any[Message], any[Throwable])
  }


  test("traceEntry") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE, AbstractLogger.ENTRY_MARKER, null.asInstanceOf[AnyRef], null)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.traceEntry()
    verify(f.mockLogger).traceEntry()
  }

  test("traceEntry enabled with params") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE, AbstractLogger.ENTRY_MARKER, null.asInstanceOf[AnyRef], null)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.traceEntry("foo", "bar")
    verify(f.mockLogger).traceEntry(null: String, "foo", "bar")
  }

  test("traceEntry disabled with params") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE, AbstractLogger.ENTRY_MARKER, null.asInstanceOf[AnyRef], null)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger.traceEntry("foo", "bar")
    verify(f.mockLogger, never).traceEntry(anyString(), anyString(), anyString())
  }

  test("traceEntry enabled with message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE, AbstractLogger.ENTRY_MARKER, null.asInstanceOf[AnyRef], null)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.traceEntry(msg)
    verify(f.mockLogger).traceEntry(eqv(msg))
  }

  test("traceEntry disabled with message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE, AbstractLogger.ENTRY_MARKER, null.asInstanceOf[AnyRef], null)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger.traceEntry(msg)
    verify(f.mockLogger, never).traceEntry(any[Message])
  }

  test("traceExit") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE, AbstractLogger.EXIT_MARKER, msg, null)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.traceExit()
    verify(f.mockLogger).traceExit()
  }

  test("traceExit with result") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE, AbstractLogger.EXIT_MARKER, msg, null)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.traceExit(result)
    verify(f.mockLogger).traceExit(result)
  }

  test("traceExit with entrymessage") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE, AbstractLogger.EXIT_MARKER, msg, null)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.traceExit(entryMsg)
    verify(f.mockLogger).traceExit(entryMsg)
  }

  test("traceExit with entrymessage and result") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE, AbstractLogger.EXIT_MARKER, msg, null)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.traceExit(entryMsg, result)
    verify(f.mockLogger).traceExit(entryMsg, result)
  }

  test("traceExit enabled with message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE, AbstractLogger.EXIT_MARKER, msg, null)).thenReturn(true)
    val logger = Logger(f.mockLogger)
    logger.traceExit(msg, result)
    verify(f.mockLogger).traceExit(eqv(msg), eqv(result))
  }

  test("traceExit disabled with message") {
    val f = fixture
    when(f.mockLogger.isEnabled(Level.TRACE, AbstractLogger.EXIT_MARKER, msg, null)).thenReturn(false)
    val logger = Logger(f.mockLogger)
    logger.traceExit(msg, result)
    verify(f.mockLogger, never).traceExit(any[Message], any[AnyRef])
  }

  test("throwing") {
    val f = fixture
    val logger = Logger(f.mockLogger)
    logger.throwing(cause)
    verify(f.mockLogger).throwing(eqv(cause))
  }

  test("throwing with level") {
    val f = fixture
    val logger = Logger(f.mockLogger)
    logger.throwing(Level.INFO, cause)
    verify(f.mockLogger).throwing(eqv(Level.INFO), eqv(cause))
  }

  test("catching") {
    val f = fixture
    val logger = Logger(f.mockLogger)
    logger.catching(cause)
    verify(f.mockLogger).catching(eqv(cause))
  }

  test("catching with level") {
    val f = fixture
    val logger = Logger(f.mockLogger)
    logger.catching(Level.INFO, cause)
    verify(f.mockLogger).catching(eqv(Level.INFO), eqv(cause))
  }

}
