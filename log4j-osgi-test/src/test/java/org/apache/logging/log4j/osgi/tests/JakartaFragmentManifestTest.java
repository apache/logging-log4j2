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
package org.apache.logging.log4j.osgi.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Validates packaged Jakarta fragment JARs expose the expected OSGi headers.
 */
class JakartaFragmentManifestTest {

    @ParameterizedTest
    @EnumSource(JakartaFragmentModule.class)
    void manifestContainsExpectedFragmentMetadata(final JakartaFragmentModule module) throws Exception {
        final Path jarPath = BundleLocations.resolveBundleJar(module.symbolicName());
        assertTrue(Files.isRegularFile(jarPath), () -> "Bundle JAR must exist for " + module.symbolicName());

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            final Manifest manifest = jarFile.getManifest();
            assertNotNull(manifest, "MANIFEST.MF must exist in " + jarPath);
            final Attributes attributes = manifest.getMainAttributes();

            assertEquals(module.symbolicName(), attributes.getValue("Bundle-SymbolicName"));
            assertEquals(module.fragmentHost(), attributes.getValue("Fragment-Host"));

            final String exportPackage = attributes.getValue("Export-Package");
            assertNotNull(exportPackage, "Export-Package must be declared");
            assertTrue(
                    exportPackage.contains(module.exportPackagePrefix()),
                    () -> "Export-Package must include " + module.exportPackagePrefix() + " but was: " + exportPackage);
        }
    }
}
