/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The converter must keep Log4j 1 variable indirection. Resolving lookups while converting captures the host.
 */
public class Log4j1ConfigurationConverterLookupTest {

    @Test
    public void systemPropertyLookupIsNotResolved() throws Exception {
        final String home = System.getProperty("user.home");
        final String xml = convert("log4j.rootLogger=INFO, FILE\n"
                + "log4j.appender.FILE=org.apache.log4j.FileAppender\n"
                + "log4j.appender.FILE.File=${user.home}/logs/app.log\n");

        assertFalse(xml.contains(home + "/logs/app.log"), xml);
        assertTrue(xml.contains("${sys:user.home}/logs/app.log"), xml);
    }

    @Test
    public void filePropertyKeepsItsNameAndTranslatesNestedSystemLookups() throws Exception {
        final String home = System.getProperty("user.home");
        final String xml = convert("log4j.rootLogger=INFO, FILE\n"
                + "app.dir=${user.home}/data\n"
                + "log4j.appender.FILE=org.apache.log4j.FileAppender\n"
                + "log4j.appender.FILE.File=${app.dir}/app.log\n");

        assertFalse(xml.contains(home), xml);
        assertTrue(xml.contains("${app.dir}/app.log"), xml);
        assertTrue(xml.contains("${sys:user.home}/data"), xml);
    }

    @Test
    public void runtimeParserStillResolvesSystemProperties() throws Exception {
        final String home = System.getProperty("user.home");
        final String properties = "log4j.rootLogger=INFO, FILE\n"
                + "log4j.appender.FILE=org.apache.log4j.FileAppender\n"
                + "log4j.appender.FILE.File=${user.home}/logs/app.log\n";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        new Log4j1ConfigurationParser()
                .buildConfigurationBuilder(new ByteArrayInputStream(properties.getBytes(StandardCharsets.ISO_8859_1)))
                .writeXmlConfiguration(output);
        final String xml = output.toString("UTF-8");

        assertTrue(xml.contains(home + "/logs/app.log"), xml);
        assertFalse(xml.contains("${sys:user.home}"), xml);
    }

    private static String convert(final String properties) throws IOException {
        final Path input = Files.createTempFile("log4j1-lookup", ".properties");
        final Path output = Files.createTempFile("log4j1-lookup", ".xml");
        try {
            Files.write(input, properties.getBytes(StandardCharsets.ISO_8859_1));
            final Log4j1ConfigurationConverter.CommandLineArguments arguments =
                    new Log4j1ConfigurationConverter.CommandLineArguments();
            arguments.setPathIn(input);
            arguments.setPathOut(output);
            Log4j1ConfigurationConverter.run(arguments);
            return new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }
}
