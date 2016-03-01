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

package org.apache.logging.log4j.core.config.xml;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * Tests automatic reconfiguration of xml
 */
public class XmlReconfigurationTest {
    private static final String BASE_DIR = "src/test/resources/";
    private static final String SUB_DIR = BASE_DIR + "LOG4J2-1301/";
    private static final int WATCH_DELAY_MS = 1200;

    private static File configFile;
    private static File appendersFile;
    private static File loggersFile;

    private static int reconfigurationCount = 0;

    private static LoggerContext ctx;

    public XmlReconfigurationTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        appendersFile = new File(BASE_DIR + "log4j-xinclude-appenders.xml");
        loggersFile = new File(BASE_DIR + "log4j-xinclude-loggers.xml");
    }

    @Before
    public void setUp() {
        reconfigurationCount = 0;
    }
    @After
    public void cleanUp(){
        this.ctx.stop();
    }

    public void setUpContext(String configFileName) {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                SUB_DIR + configFileName);
        final LoggerContext context = LoggerContext.getContext(false);
        context.reconfigure();
        this.ctx = context;
        this.configFile = new File(SUB_DIR + configFileName);
    }

    public void testVanillaReconfiguration() throws Exception{
        assertTrue(reconfigurationCount == 0);
        modifyFileAndListenForChange(configFile);
        assertTrue(reconfigurationCount == 1);
        modifyFileAndListenForChange(configFile);
        assertTrue(reconfigurationCount == 2);
    }

    @Test
    public void testWithoutOptionalFiles() throws Exception {
        setUpContext("log4j2-reconfiguration-1.xml");

        testVanillaReconfiguration();

        modifyFileAndListenForChange(appendersFile);
        assertTrue(reconfigurationCount == 2);
        modifyFileAndListenForChange(loggersFile);
        assertTrue(reconfigurationCount == 2);
    }

    @Test
    public void testWithSingleOptionalFile() throws Exception {
        setUpContext("log4j2-reconfiguration-2.xml");
        testVanillaReconfiguration();

        modifyFileAndListenForChange(appendersFile);//not watched file, should not trigger reconfig
        assertTrue(reconfigurationCount == 2);

        modifyFileAndListenForChange(loggersFile);
        assertTrue(reconfigurationCount == 3);
    }

    @Test
    public void testWithTwoOptionalFiles() throws Exception {
        setUpContext("log4j2-reconfiguration-3.xml");
        testVanillaReconfiguration();

        modifyFileAndListenForChange(appendersFile);
        assertTrue(reconfigurationCount == 3);

        modifyFileAndListenForChange(loggersFile);
        assertTrue(reconfigurationCount == 4);
    }

    @Test
    public void testWithBadOptionalFileName() throws Exception {
        setUpContext("log4j2-reconfiguration-bad-filename.xml");
        testVanillaReconfiguration();//bad value for optionalMonitorFiles should not break normal reconfiguration.
    }

    public void testWithBadFileSeparator() throws Exception {
        setUpContext("log4j2-reconfiguration-bad-separator.xml");
        testVanillaReconfiguration();//bad value for optionalMonitorFiles should not break normal reconfiguration.
    }


    private void modifyFileAndListenForChange(File file) throws InterruptedException {
        ctx.getConfiguration().removeListener(configurationListener);//in case reconfig does not occur
        ctx.getConfiguration().addListener(configurationListener);
        file.setLastModified(System.currentTimeMillis());
        Thread.sleep(WATCH_DELAY_MS);
    }

    private ConfigurationListener configurationListener = new ConfigurationListener() {
        @Override
        public void onChange(Reconfigurable reconfigurable) {
            ++reconfigurationCount;
        }
    };

}
