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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

public class LoggerContextChangeTest {
    private static String targetFileName = "log4j2-3182.xml";
    private static String targetErrorFileName = "log4j2-3182-error.xml";
    private static String tmpFileName = "log4j2-3182-tmp.xml";
    private static String destDir = "target/test-classes/";

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("log4j2.configurationFile", "classpath:" + targetFileName);
    }


    @Test
    public void onChangeTest() {
        String errorFileName = destDir + targetErrorFileName;
        String originFileName = destDir + targetFileName;
        String tmpFile = destDir + tmpFileName;
        wait(1);
        updateConfigFileModTime(originFileName, errorFileName, tmpFile);
        wait(5);
        Set<Thread> allThreads = Thread.getAllStackTraces().keySet();
        Integer kafkaThreads = 0;
        Integer consoleThreads = 0;
        for (Thread thread:allThreads) {
            if (thread.getName().contains("AsyncKafka")) {
                kafkaThreads++;
            }
            if (thread.getName().contains("AsyncCONSOLE")) {
                consoleThreads++;
            }
        }
        Assert.assertTrue(kafkaThreads <= 1);
        Assert.assertTrue(consoleThreads <= 1);
        updateConfigFileModTime(originFileName, tmpFile, errorFileName);
    }

    private void wait(int seconds) {
        try {
            Logger logger = LogManager.getLogger();
            logger.info("Test");
            Thread.sleep(seconds * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void updateConfigFileModTime(String originFileName, String destFileName, String tmpFileName) {
        File originFile = new File(originFileName);
        if (originFile.exists()) {
            originFile.renameTo(new File(tmpFileName));
        }
        File destFile = new File(destFileName);
        if (destFile.exists()) {
            destFile.setLastModified(System.currentTimeMillis());
            destFile.renameTo(new File(originFileName));
        }
    }
}
