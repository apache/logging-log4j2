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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by rgoers on 1/1/17.
 */
public class CronRolloverApp {


    private static Logger logger;

    public static void main(String[] args) {
        System.setProperty("log4j.configurationFile", "target/test-classes/log4j-cronRolloverApp.xml");
        logger = LogManager.getLogger(CronRolloverApp.class);
        try {
            for (int i = 1; i <= 240; i++) {
                logger.info("Hello");
                Thread.sleep(1 * 1000);
            }
        } catch (Exception e) {
            //e.printStackTrace();
            logger.error("Excepcion general", e);
        }
    }
}
