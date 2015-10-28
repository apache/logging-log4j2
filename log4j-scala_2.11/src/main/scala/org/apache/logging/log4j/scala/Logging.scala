package org.apache.logging.log4j.scala

import org.apache.logging.log4j.LogManager

trait Logging {

  protected lazy val logger: Logger =
    new Logger(LogManager.getContext(getClass.getClassLoader, false).getLogger(getClass.getName))

}