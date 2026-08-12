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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Smoke-tests that Jakarta fragment bundles install in an OSGi framework and declare the expected host.
 */
abstract class AbstractJakartaFragmentInstallTest {

    private BundleContext bundleContext;

    @RegisterExtension
    final OsgiExt osgi;

    AbstractJakartaFragmentInstallTest(final FrameworkFactory frameworkFactory) {
        osgi = new OsgiExt(frameworkFactory);
    }

    @BeforeEach
    void before() {
        bundleContext = osgi.getFramework().getBundleContext();
    }

    @ParameterizedTest
    @EnumSource(JakartaFragmentModule.class)
    void jakartaFragmentInstallsWithExpectedHost(final JakartaFragmentModule module) throws BundleException {
        final Bundle fragment = installBundle(module.symbolicName());

        assertEquals(module.symbolicName(), fragment.getSymbolicName());
        assertEquals(module.fragmentHost(), fragment.getHeaders().get("Fragment-Host"));
        assertNotNull(fragment.getHeaders().get("Export-Package"));
        assertEquals(Bundle.INSTALLED, fragment.getState());

        fragment.uninstall();
        assertEquals(Bundle.UNINSTALLED, fragment.getState());
    }

    private Bundle installBundle(final String symbolicName) throws BundleException {
        final String url = String.format("link:classpath:%s.link", symbolicName);
        return bundleContext.installBundle(url);
    }
}
