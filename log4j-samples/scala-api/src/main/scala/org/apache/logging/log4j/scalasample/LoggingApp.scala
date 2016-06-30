package org.apache.logging.log4j.scalasample

import org.apache.logging.log4j.scala.Logging

object LoggingApp extends App with Logging {

  val s1 = "foo"
  val s2 = "bar"
  logger.info(s"Hello, world: $s1 $s2")

  logger.traceEntry()
  logger.traceEntry(s1, s2)
  val entryMessage = logger.traceEntry(logger.messageFactory.newMessage("foobar": CharSequence))

  logger.traceExit()
  logger.traceExit(s2)
  logger.traceExit(entryMessage)
  logger.traceExit(entryMessage, s2)
  logger.traceExit(logger.messageFactory.newMessage("bonsai": CharSequence), s2)

}