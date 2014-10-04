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
package org.apache.logging.slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import static org.junit.Assert.*;

/**
 *
 */
public class SerializeTest {


    private static final String CONFIG = "log4j-test1.xml";

    @ClassRule
    public static final InitialLoggerContext CTX = new InitialLoggerContext(CONFIG);

    Logger logger = LoggerFactory.getLogger("LoggerTest");
    XLogger xlogger = XLoggerFactory.getXLogger("LoggerTest");

    @Test
    public void testLogger() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(logger);
        final byte[] data = baos.toByteArray();
        assertNotNull("No data", data);
        assertTrue("No data", data.length > 0);
        final ByteArrayInputStream bais = new ByteArrayInputStream(data);
        final ObjectInputStream ois = new ObjectInputStream(bais);
        final Logger copy = (org.slf4j.Logger) ois.readObject();
        assertNotNull("Unable to restore logger", copy);
    }
}
