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
package org.apache.logging.log4j.osgi;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

/**
 * Tests loading a bundle into an OSGi container.
 * <p>
 * Requires that "mvn package" has been previously run, otherwise test fails its JUnit {@link Assume}.
 * </p>
 * <p>
 * For example, on Windows: "mvn clean package -DskipTests & mvn test"
 * </p>
 */
public abstract class AbstractLoadBundleTest {

    protected static Framework OsgiFramework;

    /**
     * Uninstalls the OSGi framework.
     * 
     * @throws BundleException
     */
    @AfterClass
    public static void afterClass() throws BundleException {
        if (OsgiFramework != null) {
            OsgiFramework.stop();
            OsgiFramework = null;
        }
    }

    private final BundleTestInfo bundleTestInfo;

    /**
     * Constructs a test for a given bundle.
     */
    public AbstractLoadBundleTest() {
        super();
        this.bundleTestInfo = new BundleTestInfo();
    }

    /**
     * Called before each @Test.
     */
    @Before
    public void before() {
        final String bundlePath = getBundlePath();
        Assume.assumeNotNull(bundlePath);
        final File file = new File(bundlePath);
        Assume.assumeTrue("File does not exist: " + file.getAbsolutePath() + ". Run 'mvn package' before 'mvn test'",
                file.exists());
    }

    protected String getBundlePath() {
        return "target/" + bundleTestInfo.getArtifactId() + "-" + bundleTestInfo.getVersion() + ".jar";
    }

    /**
     * Gets the expected bundle symbolic name.
     * 
     * @return the expected bundle symbolic name.
     */
    public String getExpectedBundleSymbolicName() {
        return "org.apache.logging." + bundleTestInfo.getArtifactId();
    }

    /**
     * Loads, starts, and stops a bundle.
     * 
     * @throws BundleException
     */
    @Test
    public void testLoadStartStop() throws BundleException {
        final BundleContext bundleContext = OsgiFramework.getBundleContext();
        final Bundle bundle = bundleContext.installBundle("file:" + getBundlePath());
        Assert.assertNotNull("Error loading bundle: null returned", bundle);
        Assert.assertEquals("Error loading bundle: symbolic name mismatch", getExpectedBundleSymbolicName(),
                bundle.getSymbolicName());
        Assert.assertEquals("Bundle is not in INSTALLED state", Bundle.INSTALLED, bundle.getState());

        // sanity check: start and stop bundle
        bundle.start();
        Assert.assertEquals("Bundle is not in ACTIVE state", Bundle.ACTIVE, bundle.getState());
        bundle.stop();
        Assert.assertEquals("Bundle is not in RESOLVED state", Bundle.RESOLVED, bundle.getState());
        bundle.start();
        Assert.assertEquals("Bundle is not in ACTIVE state", Bundle.ACTIVE, bundle.getState());
        bundle.stop();
        Assert.assertEquals("Bundle is not in RESOLVED state", Bundle.RESOLVED, bundle.getState());
    }

}
