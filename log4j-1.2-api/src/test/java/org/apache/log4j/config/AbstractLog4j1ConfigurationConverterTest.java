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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public abstract class AbstractLog4j1ConfigurationConverterTest {

    protected static List<Path> getPaths(final String root) throws IOException {
        final List<Path> paths = new ArrayList<>();
        Files.walkFileTree(Paths.get(root), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                paths.add(file.toAbsolutePath());
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }

    public AbstractLog4j1ConfigurationConverterTest() {}

    @Test
    public void test(Path path) throws Exception {
        final Path tempFile = Files.createTempFile("log4j2", ".xml");
        try {
            final Log4j1ConfigurationConverter.CommandLineArguments cla =
                    new Log4j1ConfigurationConverter.CommandLineArguments();
            cla.setPathIn(path);
            cla.setPathOut(tempFile);
            Log4j1ConfigurationConverter.run(cla);
            checkWellFormedXml(tempFile);
            checkUnnecessaryEscaping(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void checkUnnecessaryEscaping(final Path tempFile) throws IOException {
        for (String line : Files.readAllLines(tempFile)) {
            assertFalse(line.endsWith("&#xd;"));
        }
    }

    private void checkWellFormedXml(final Path xmlFilePath)
            throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(xmlFilePath.toUri().toString());
    }
}
