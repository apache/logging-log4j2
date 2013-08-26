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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.ThreadContextMapFilter;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 */
@RunWith(Parameterized.class)
public class XMLConfigurationTest {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { "log4j-test1.xml", "target/test.log", false },
                { "log4j-xinclude.xml", "target/test.log", true } });
    }

    private final String configFile;
    private final String logFile;
    private final boolean xinclude;

    public XMLConfigurationTest(String configFile, String logFile, boolean validate) {
        super();
        this.configFile = configFile;
        this.logFile = logFile;
        this.xinclude = validate;
    }

    @Test
    public void logToFile() throws Exception {
        final FileOutputStream fos = new FileOutputStream(logFile, false);
        fos.flush();
        fos.close();
        final Logger logger = LogManager.getLogger("org.apache.logging.log4j.test2.Test");
        logger.debug("This is a test");
        final BufferedReader is = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
        try {
            int count = 0;
            String str = "";
            while (is.ready()) {
                str = is.readLine();
                ++count;
            }
            assertTrue("Incorrect count " + count, count == 1);
            assertTrue("Bad data", str.endsWith("This is a test"));
        } finally {
            is.close();
        }
    }

    @Before
    public void setUp() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, configFile);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        final Configuration config = ctx.getConfiguration();
        if (config instanceof XMLConfiguration) {
            final String name = config.getName();
            if (name == null || !name.equals("XMLConfigTest")) {
                ctx.reconfigure();
            }
        } else {
            ctx.reconfigure();
        }
    }

    @After
    public void tearDown() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testConfiguredAppenders() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        final Configuration c = ctx.getConfiguration();
        final Map<String, Appender> apps = c.getAppenders();
        assertNotNull(apps);
        assertEquals(3, apps.size());
    }

    @Test
    public void testLogger() {
        final Logger logger = LogManager.getLogger("org.apache.logging.log4j.test1.Test");
        assertTrue(logger instanceof org.apache.logging.log4j.core.Logger);
        final org.apache.logging.log4j.core.Logger l = (org.apache.logging.log4j.core.Logger) logger;
        assertEquals(Level.DEBUG, l.getLevel());
        final int filterCount = l.filterCount();
        assertTrue("number of filters - " + filterCount, filterCount == 1);
        final Iterator<Filter> iter = l.getFilters();
        final Filter filter = iter.next();
        assertTrue(filter instanceof ThreadContextMapFilter);
        final Map<String, Appender> appenders = l.getAppenders();
        assertNotNull(appenders);
        assertTrue("number of appenders = " + appenders.size(), appenders.size() == 1);
        final Appender a = appenders.get("STDOUT");
        assertNotNull(a);
        assertEquals(a.getName(), "STDOUT");
    }

    @Test
    @Ignore
    public void testValidation() throws SAXException, IOException, ParserConfigurationException {
        // For now, XInclude and validation do not work together.
        Assume.assumeFalse(this.xinclude);
        URL schemaFile = ClassLoader.getSystemResource("Log4j-config.xsd");
        URL xmlFile = ClassLoader.getSystemResource(configFile);
        Assert.assertNotNull(schemaFile);
        Document doc = XMLConfiguration.newDocumentBuilder().parse(xmlFile.toString());
        Source xmlSource = new DOMSource(doc);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        validator.validate(xmlSource);
    }

}
