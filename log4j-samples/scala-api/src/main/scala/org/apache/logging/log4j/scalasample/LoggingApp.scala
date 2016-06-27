package org.apache.logging.log4j.scalasample

import org.apache.logging.log4j.scala.Logging

object LoggingApp extends App with Logging {

  val i = 17
  logger.info(s"Hello, world: $i")

}