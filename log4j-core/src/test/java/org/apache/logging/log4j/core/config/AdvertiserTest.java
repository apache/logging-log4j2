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
import java.util.Map;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class AdvertiserTest {

    private static final String CONFIG = "log4j-advertiser.xml";
    private static final String STATUS_LOG = "target/status.log";

    @BeforeClass
    public static void setupClass() {
        final File file = new File(STATUS_LOG);
        file.delete();
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        final LoggerContext ctx = LoggerContext.getContext();
        final Configuration config = ctx.getConfiguration();
        if (config instanceof XmlConfiguration) {
            final String name = config.getName();
            if (name == null || !name.equals("XMLConfigTest")) {
                ctx.reconfigure();
            }
        } else {
            ctx.reconfigure();
        }
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
        final File file = new File(STATUS_LOG);
        file.delete();
    }

    private void verifyExpectedEntriesAdvertised(final Map<Object, Map<String, String>> entries) {
        boolean foundFile1 = false;
        boolean foundFile2 = false;
        boolean foundSocket1 = false;
        boolean foundSocket2 = false;
        for (final Map<String, String>entry:entries.values()) {
            if (foundFile1 && foundFile2 && foundSocket1 && foundSocket2) {
               break;
            }
            if (entry.get("name").equals("File1")) {
                foundFile1 = true;
            }
            if (entry.get("name").equals("File2")) {
                foundFile2 = true;
            }
            if (entry.get("name").equals("Socket1")) {
                foundSocket1 = true;
            }
            if (entry.get("name").equals("Socket2")) {
                foundSocket2 = true;
            }
        }
        assertTrue("Entries for File1 appender do not exist", foundFile1);
        assertTrue("Entries for File2 appender exist", !foundFile2);
        assertTrue("Entries for Socket1 appender do not exist", foundSocket1);
        assertTrue("Entries for Socket2 appender exist", !foundSocket2);
    }

    @Test
    public void testAdvertisementsFound() {
        verifyExpectedEntriesAdvertised(InMemoryAdvertiser.getAdvertisedEntries());
    }

    @Test
    public void testAdvertisementsRemovedOnConfigStop() {
        verifyExpectedEntriesAdvertised(InMemoryAdvertiser.getAdvertisedEntries());

        final LoggerContext ctx = LoggerContext.getContext();
        ctx.stop();

        final Map<Object, Map<String, String>> entries = InMemoryAdvertiser.getAdvertisedEntries();
        assertTrue("Entries found: " + entries, entries.isEmpty());

        //reconfigure for subsequent testing
        ctx.start();
    }

    @Test
    public void testAdvertisementsAddedOnReconfigAfterStop() {
        verifyExpectedEntriesAdvertised(InMemoryAdvertiser.getAdvertisedEntries());

        final LoggerContext ctx = LoggerContext.getContext();
        ctx.stop();

        final Map<Object, Map<String, String>> entries = InMemoryAdvertiser.getAdvertisedEntries();
        assertTrue("Entries found: " + entries, entries.isEmpty());

        ctx.start();

        verifyExpectedEntriesAdvertised(InMemoryAdvertiser.getAdvertisedEntries());
    }
}
