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
package org.apache.logging.log4j.core.appender;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.categories.Layouts;
import org.apache.logging.log4j.core.CoreLoggerContexts;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.xml.sax.SAXException;

/**
 * Tests XML validation for a "compact" XML file, no extra spaces or end of lines.
 */
@Ignore
@Category(Layouts.Xml.class)
public class XmlCompactFileAsyncAppenderValidationTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "XmlCompactFileAsyncAppenderValidationTest.xml");
    }

    @Test
    public void validateXmlSchemaSimple() throws Exception {
        final File file = new File("target", "XmlCompactFileAsyncAppenderValidationTest.log.xml");
        file.delete();
        final Logger log = LogManager.getLogger("com.foo.Bar");
        log.warn("Message 1");
        log.info("Message 2");
        log.debug("Message 3");
        CoreLoggerContexts.stopLoggerContext(file); // stop async thread
        this.validateXmlSchema(file);
    }

    @Test
    public void validateXmlSchemaNoEvents() throws Exception {
        final File file = new File("target", "XmlCompactFileAsyncAppenderValidationTest.log.xml");
        file.delete();
        CoreLoggerContexts.stopLoggerContext(file); // stop async thread
        this.validateXmlSchema(file);
    }

    private void validateXmlSchema(final File file) throws SAXException, IOException {
        final URL schemaFile = this.getClass().getClassLoader().getResource("Log4j-events.xsd");
        final Source xmlFile = new StreamSource(file);
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Schema schema = schemaFactory.newSchema(schemaFile);
        final Validator validator = schema.newValidator();
        validator.validate(xmlFile);
    }

}
