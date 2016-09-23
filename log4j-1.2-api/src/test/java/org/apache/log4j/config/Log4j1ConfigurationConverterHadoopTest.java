package org.apache.log4j.config;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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

@Ignore("Some files cannot be converted!")
@RunWith(Parameterized.class)
public class Log4j1ConfigurationConverterHadoopTest {

    @Parameterized.Parameters(name = "{0}")
    public static List<Path> data() throws IOException {
        final String root = "src/test/resources/config-1.2/hadoop";
        final List<Path> paths = new ArrayList<>();
        Files.walkFileTree(Paths.get(root), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                paths.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }

    private final Path pathIn;

    public Log4j1ConfigurationConverterHadoopTest(final Path path) {
        super();
        this.pathIn = path;
    }

    @Test
    public void test() {
        final Log4j1ConfigurationConverter.CommandLineArguments cla = new Log4j1ConfigurationConverter.CommandLineArguments();
        cla.setPathIn(pathIn);
        Log4j1ConfigurationConverter.run(cla);
    }
}
