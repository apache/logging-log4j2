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
package org.apache.logging.log4j.async;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log4J2AsyncLogger {

    private static final Logger logger = LogManager.getLogger(Log4J2AsyncLogger.class);

    public static void main(String[] args) {

        Log4J2AsyncLogger myLog = new Log4J2AsyncLogger();
        myLog.getLog("Log4j2 Log");

    }

    private void getLog(String param){

        logger.info("This is a info log");

        // Previously, need to check the log level log to increase performance
        if(logger.isDebugEnabled()){
            logger.debug("This is debug log with param : " + param);
        }

        if(logger.isWarnEnabled()){
            logger.info("This is warn log with param : " + param);
        }

        // In Java 8, No need to check the log level, we can do this
        while (true) //for test rolling file
            logger.debug("Hello print {}", () -> getValue());
    }

    static String getValue() {
        return "Debug Log";
    }
}
