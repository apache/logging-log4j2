package org.apache.logging.log4j.scala

import org.apache.logging.log4j.message.{SimpleMessage, Message, ParameterizedMessageFactory}
import org.apache.logging.log4j.spi.ExtendedLogger
import org.apache.logging.log4j.{MarkerManager, Level, Marker}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString, eq => eqv}
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

@RunWith(classOf[JUnitRunner])
class LoggerTest extends FunSuite with Matchers with MockitoSugar {

  val stringMsg = "string msg"
  val msg = new SimpleMessage("simple msg")
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

  def buildMockLogger: ExtendedLogger = {
    val mockLogger = mock[ExtendedLogger]
    when(mockLogger.getMessageFactory).thenReturn(new ParameterizedMessageFactory)
    mockLogger
  }

}
