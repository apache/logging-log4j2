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
package org.apache.logging.log4j.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.logging.log4j.core.net.MailManagerFactory;
import org.junit.jupiter.api.Test;

class OsgiMetadataTest {

    private static final String EXPECTED_SYMBOLIC_NAME = "org.apache.logging.log4j.jakarta.smtp";
    private static final String EXPECTED_FRAGMENT_HOST = "org.apache.logging.log4j.core";
    private static final String EXPECTED_FACTORY = "org.apache.logging.log4j.smtp.SmtpManager$SMTPManagerFactory";

    @Test
    void manifestContainsExpectedOsgiMetadata() throws Exception {
        final Path manifestPath = Paths.get("target/classes/META-INF/MANIFEST.MF");
        assertTrue(Files.isRegularFile(manifestPath), "MANIFEST.MF must exist after bnd-process");
        try (InputStream in = Files.newInputStream(manifestPath)) {
            final Manifest manifest = new Manifest(in);
            final Attributes attributes = manifest.getMainAttributes();
            assertEquals(EXPECTED_SYMBOLIC_NAME, attributes.getValue("Bundle-SymbolicName"));
            assertEquals(EXPECTED_FRAGMENT_HOST, attributes.getValue("Fragment-Host"));
        }
    }

    @Test
    void serviceProviderRegistersJakartaSmtpManagerFactory() throws Exception {
        final Path servicePath =
                Paths.get("target/classes/META-INF/services/org.apache.logging.log4j.core.net.MailManagerFactory");
        assertTrue(Files.isRegularFile(servicePath), "MailManagerFactory service file must exist");
        final String contents = new String(Files.readAllBytes(servicePath), StandardCharsets.UTF_8);
        assertTrue(contents.contains(EXPECTED_FACTORY));
    }

    @Test
    void serviceLoaderFindsJakartaSmtpManagerFactory() {
        final Iterator<MailManagerFactory> factories =
                ServiceLoader.load(MailManagerFactory.class).iterator();
        MailManagerFactory factory = null;
        while (factories.hasNext()) {
            factory = factories.next();
            break;
        }
        assertNotNull(factory);
        assertEquals(SmtpManager.SMTPManagerFactory.class, factory.getClass());
    }
}
