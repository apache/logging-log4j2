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
package org.apache.logging.log4j.core.appender.mom.jakarta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.junit.Test;

/**
 * Verifies OSGi metadata generated from {@code bnd-module-name} and {@code Fragment-Host} POM properties.
 */
public class OsgiMetadataTest {

    private static final String EXPECTED_SYMBOLIC_NAME = "org.apache.logging.log4j.jakarta.jms";
    private static final String EXPECTED_FRAGMENT_HOST = "org.apache.logging.log4j.core";

    @Test
    public void testOsgiManifestMetadata() throws Exception {
        final java.nio.file.Path manifestPath = Paths.get("target/classes/META-INF/MANIFEST.MF");
        assertTrue("MANIFEST.MF must exist after bnd-process", Files.isRegularFile(manifestPath));
        try (InputStream in = Files.newInputStream(manifestPath)) {
            final Manifest manifest = new Manifest(in);
            final Attributes attributes = manifest.getMainAttributes();
            assertEquals(EXPECTED_SYMBOLIC_NAME, attributes.getValue("Bundle-SymbolicName"));
            assertEquals(EXPECTED_FRAGMENT_HOST, attributes.getValue("Fragment-Host"));
        }
    }
}
