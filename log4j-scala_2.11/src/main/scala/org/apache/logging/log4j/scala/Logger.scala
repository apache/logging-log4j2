package org.apache.logging.log4j.scala

import org.apache.logging.log4j.message.Message
import org.apache.logging.log4j.spi.ExtendedLogger
import org.apache.logging.log4j.{Level, Marker}

import scala.language.experimental.macros

class Logger(val delegate: ExtendedLogger) {

  private final val FQCN = classOf[Logger].getName

  def fatal(marker: Marker, message: Message): Unit =
  macro LoggerMacro.fatalMarkerMsg

  def fatal(marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.fatalMarkerMsgThrowable

  def fatal(marker: Marker, message: String): Unit =
  macro LoggerMacro.fatalMarkerString

  def fatal(marker: Marker, message: String, cause: Throwable): Unit =
  macro LoggerMacro.fatalMarkerStringThrowable

  def fatal(message: Message): Unit =
  macro LoggerMacro.fatalMsg

  def fatal(message: String): Unit =
  macro LoggerMacro.fatalString

  def fatal(message: Message, cause: Throwable): Unit =
  macro LoggerMacro.fatalMsgThrowable

  def fatal(message: String, cause: Throwable): Unit =
  macro LoggerMacro.fatalStringThrowable

  def error(marker: Marker, message: Message): Unit =
  macro LoggerMacro.errorMarkerMsg

  def error(marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.errorMarkerMsgThrowable

  def error(marker: Marker, message: String): Unit =
  macro LoggerMacro.errorMarkerString

  def error(marker: Marker, message: String, cause: Throwable): Unit =
  macro LoggerMacro.errorMarkerStringThrowable

  def error(message: Message): Unit =
  macro LoggerMacro.errorMsg

  def error(message: String): Unit =
  macro LoggerMacro.errorString

  def error(message: Message, cause: Throwable): Unit =
  macro LoggerMacro.errorMsgThrowable

  def error(message: String, cause: Throwable): Unit =
  macro LoggerMacro.errorStringThrowable

  def warn(marker: Marker, message: Message): Unit =
  macro LoggerMacro.warnMarkerMsg

  def warn(marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.warnMarkerMsgThrowable

  def warn(marker: Marker, message: String): Unit =
  macro LoggerMacro.warnMarkerString

  def warn(marker: Marker, message: String, cause: Throwable): Unit =
  macro LoggerMacro.warnMarkerStringThrowable

  def warn(message: Message): Unit =
  macro LoggerMacro.warnMsg

  def warn(message: String): Unit =
  macro LoggerMacro.warnString

  def warn(message: Message, cause: Throwable): Unit =
  macro LoggerMacro.warnMsgThrowable

  def warn(message: String, cause: Throwable): Unit =
  macro LoggerMacro.warnStringThrowable

  def info(marker: Marker, message: Message): Unit =
  macro LoggerMacro.infoMarkerMsg

  def info(marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.infoMarkerMsgThrowable

  def info(marker: Marker, message: String): Unit =
  macro LoggerMacro.infoMarkerString

  def info(marker: Marker, message: String, cause: Throwable): Unit =
  macro LoggerMacro.infoMarkerStringThrowable

  def info(message: Message): Unit =
  macro LoggerMacro.infoMsg

  def info(message: String): Unit =
  macro LoggerMacro.infoString

  def info(message: Message, cause: Throwable): Unit =
  macro LoggerMacro.infoMsgThrowable

  def info(message: String, cause: Throwable): Unit =
  macro LoggerMacro.infoStringThrowable

  def debug(marker: Marker, message: Message): Unit =
  macro LoggerMacro.debugMarkerMsg

  def debug(marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.debugMarkerMsgThrowable

  def debug(marker: Marker, message: String): Unit =
  macro LoggerMacro.debugMarkerString

  def debug(marker: Marker, message: String, cause: Throwable): Unit =
  macro LoggerMacro.debugMarkerStringThrowable

  def debug(message: Message): Unit =
  macro LoggerMacro.debugMsg

  def debug(message: String): Unit =
  macro LoggerMacro.debugString

  def debug(message: Message, cause: Throwable): Unit =
  macro LoggerMacro.debugMsgThrowable

  def debug(message: String, cause: Throwable): Unit =
  macro LoggerMacro.debugStringThrowable

  def trace(marker: Marker, message: Message): Unit =
  macro LoggerMacro.traceMarkerMsg

  def trace(marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.traceMarkerMsgThrowable

  def trace(marker: Marker, message: String): Unit =
  macro LoggerMacro.traceMarkerString

  def trace(marker: Marker, message: String, cause: Throwable): Unit =
  macro LoggerMacro.traceMarkerStringThrowable

  def trace(message: Message): Unit =
  macro LoggerMacro.traceMsg

  def trace(message: String): Unit =
  macro LoggerMacro.traceString

  def trace(message: Message, cause: Throwable): Unit =
  macro LoggerMacro.traceMsgThrowable

  def trace(message: String, cause: Throwable): Unit =
  macro LoggerMacro.traceStringThrowable

  def log(level: Level, marker: Marker, message: Message): Unit =
  macro LoggerMacro.logMarkerMsg

  def log(level: Level, marker: Marker, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.logMarkerMsgThrowable

  def log(level: Level, marker: Marker, message: String): Unit =
  macro LoggerMacro.logMarkerString

  def log(level: Level, marker: Marker, message: String, cause: Throwable): Unit =
  macro LoggerMacro.logMarkerStringThrowable

  def log(level: Level, message: Message): Unit =
  macro LoggerMacro.logMsg

  def log(level: Level, message: String): Unit =
  macro LoggerMacro.logString

  def log(level: Level, message: Message, cause: Throwable): Unit =
  macro LoggerMacro.logMsgThrowable

  def log(level: Level, message: String, cause: Throwable): Unit =
  macro LoggerMacro.logStringThrowable

  def isEnabled(level: Level): Boolean = delegate.isEnabled(level)

  def isEnabled(level: Level, marker: Marker): Boolean = delegate.isEnabled(level, marker)

  def level: Level = delegate.getLevel

  def name: String = delegate.getName

  /** Should normally not be used directly from application code, but needs to be public for access by macros.
    *
    * @param level    log level
    * @param marker   marker or `null`
    * @param message  message
    * @param cause    cause or `null`
    */
  def logMessage(level: Level, marker: Marker, message: Message, cause: Throwable): Unit = {
    delegate.logIfEnabled(FQCN, level, marker, message, cause)
  }

  /** Should normally not be used directly from application code, but needs to be public for access by macros.
    *
    * @param level    log level
    * @param marker   marker or `null`
    * @param message  message
    * @param cause    cause or `null`
    */
  def logMessage(level: Level, marker: Marker, message: String, cause: Throwable): Unit = {
    delegate.logIfEnabled(FQCN, level, marker, message, cause)
  }

}
