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
package org.apache.logging.log4j.core.config;

import java.io.File;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Performs reconfiguration against bad configurations.
 *
 */
@Disabled // Remove this when LOG4J2-2240 is addressed.
public class ReconfigurationFailureTest {

    LoggerContext loggerContext;

    @BeforeEach
    public void setup() {
        loggerContext = (LoggerContext) LogManager.getContext();
    }

    @AfterEach
    public void stopExecutor() throws InterruptedException {
        loggerContext.stop();
    }

    @Test
    public void setNonExistant() throws Exception {

       URI original = loggerContext.getConfigLocation();
       URI nonExistant = new File("target/file.does.not.exist.xml").toURI();
       loggerContext.setConfigLocation(nonExistant);
       assertEquals(original, loggerContext.getConfigLocation(), "URI after failure is not the original");
    }


    @Test
    public void setInvalidXML() throws Exception {

        URI original = loggerContext.getConfigLocation();
        URI nonExistant = new File("target/InvalidXML.xml").toURI();
        loggerContext.setConfigLocation(nonExistant);
        assertEquals(original, loggerContext.getConfigLocation(), "URI after failure is not the original");
    }

    @Test
    public void setInvalidConfig() throws Exception {

        URI original = loggerContext.getConfigLocation();
        URI nonExistant = new File("target/InvalidConfig.xml").toURI();
        loggerContext.setConfigLocation(nonExistant);
        assertEquals(original, loggerContext.getConfigLocation(), "URI after failure is not the original");
    }



}
