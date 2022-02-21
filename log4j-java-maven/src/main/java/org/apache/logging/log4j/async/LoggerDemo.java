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

public class LoggerDemo {

    private static final Logger logger = LogManager.getLogger(LoggerDemo.class);

    public static void main(String[] args) {

        LoggerDemo myLog = new LoggerDemo();

        // Previously, need to check the log level log to increase performance
        myLog.getLog("Log4j2 Log");

        // In Java 8, No need to check the log level, we can do this
        while (true) //test rolling file
            logger.debug("Hello print {}", () -> getValue());
    }

    private void getLog(String param){

        if(logger.isDebugEnabled()){
            logger.debug("This is debug log with param : " + param);
        }

        if(logger.isInfoEnabled()){
            logger.info("This is info log with param : " + param);
        }

        logger.warn("This is warn message");
        logger.error("This is error message");
        logger.fatal("This is fatal message");
    }

    static String getValue() {
        return "Debug Log";
    }
}
