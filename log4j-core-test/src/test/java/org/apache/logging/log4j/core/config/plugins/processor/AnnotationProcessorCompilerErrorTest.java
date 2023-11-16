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
package org.apache.logging.log4j.core.config.plugins.processor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.core.test.Compiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junitpioneer.jupiter.Issue;

@Issue("LOG4J2-3609")
// only enabled for explicit testing of what may be a javac bug
// mvn test -Dtest=AnnotationProcessorCompilerErrorTest'#'testMissingExternalAnnotationIsSupported
@EnabledIfSystemProperty(
        named = "test",
        matches = "AnnotationProcessorCompilerErrorTest#testMissingExternalAnnotationIsSupported")
class AnnotationProcessorCompilerErrorTest {
    @Test
    void testMissingExternalAnnotationIsSupported() throws Exception {
        final Path sourceDirectory = Paths.get("target", "test-classes", "LOG4J2-3609");
        // set up classpath directories
        final Path annotationDirectory = Files.createTempDirectory("annotation");
        final Path coreDirectory = Files.createTempDirectory("core");
        final Path bugDirectory = Files.createTempDirectory("bug");
        final Path processorDirectory = Files.createTempDirectory("processor");

        // Create a stub annotation processor
        final Path originalProcessor = sourceDirectory.resolve("MyAnnotationProcessor.java.source");
        final Path myProcessor = sourceDirectory.resolve("MyAnnotationProcessor.java");
        Files.move(originalProcessor, myProcessor);
        try {
            Compiler.compile(myProcessor.toFile(), "-proc:none", "-d", processorDirectory.toString());
        } finally {
            Files.move(myProcessor, originalProcessor);
        }

        // Compile the annotation and keep the class in a dedicated directory.
        // javac -d target/annotation src/MyAnnotation.java
        final Path originalAnnotation = sourceDirectory.resolve("MyAnnotation.java.source");
        final Path myAnnotation = sourceDirectory.resolve("MyAnnotation.java");
        Files.move(originalAnnotation, myAnnotation);
        try {
            Compiler.compile(
                    myAnnotation.toFile(),
                    "-d",
                    annotationDirectory.toString(),
                    "-processorpath",
                    processorDirectory.toString(),
                    "-processor",
                    "MyAnnotationProcessor");
        } finally {
            Files.move(myAnnotation, originalAnnotation);
        }

        // Compile the annotated class and save it in a dedicated directory.
        // javac -d target/core -cp target/annotation src/MyAnnotatedClass.java
        final Path originalAnnotatedClass = sourceDirectory.resolve("MyAnnotatedClass.java.source");
        final Path myAnnotatedClass = sourceDirectory.resolve("MyAnnotatedClass.java");
        Files.move(originalAnnotatedClass, myAnnotatedClass);
        try {
            Compiler.compile(
                    myAnnotatedClass.toFile(),
                    "-d",
                    coreDirectory.toString(),
                    "-cp",
                    annotationDirectory.toString(),
                    "-processorpath",
                    processorDirectory.toString(),
                    "-processor",
                    "MyAnnotationProcessor");
        } finally {
            Files.move(myAnnotatedClass, originalAnnotatedClass);
        }

        // Compile the empty subclass, which depends transitively on MyAnnotation,
        // without including MyAnnotation.class in the classpath.
        // javac -d target/bug -cp target/core:target/log4j/* src/MyEmptySubClass.java
        final Path originalSubclass = sourceDirectory.resolve("MyEmptySubClass.java.source");
        final Path mySubclass = sourceDirectory.resolve("MyEmptySubClass.java");
        Files.move(originalSubclass, mySubclass);
        try {
            Compiler.compile(
                    mySubclass.toFile(),
                    "-d",
                    bugDirectory.toString(),
                    "-cp",
                    coreDirectory.toString(),
                    "-processorpath",
                    processorDirectory.toString(),
                    "-processor",
                    "MyAnnotationProcessor");
        } finally {
            Files.move(mySubclass, originalSubclass);
        }
    }
}
