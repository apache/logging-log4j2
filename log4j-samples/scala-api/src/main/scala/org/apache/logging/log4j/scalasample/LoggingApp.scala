package org.apache.logging.log4j.scalasample

import org.apache.logging.log4j.message.MessageFactory2
import org.apache.logging.log4j.scala.Logging

object LoggingApp extends App with Logging {

  val s1 = "foo"
  val s2 = "bar"
  logger.info(s"Hello, world: $s1 $s2")

  logger.traceEntry()
  logger.traceEntry(s1, s2)
  val entryMessage = logger.traceEntry(logger.delegate.getMessageFactory.asInstanceOf[MessageFactory2].newMessage("foobar": CharSequence))

  logger.traceExit()
  logger.traceExit(s2)
  logger.traceExit(entryMessage)
  logger.traceExit(entryMessage, s2)
  logger.traceExit(logger.delegate.getMessageFactory.asInstanceOf[MessageFactory2].newMessage("bonsai": CharSequence), s2)

}