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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Tests loading a bundle into an OSGi container. Also test a basic Log4J 'setup' in an OSGi container.
 * <p>
 * Requires that "mvn package" has been previously run, otherwise test fails its JUnit {@link Assume}.
 * </p>
 * <p>
 * For example, on Windows: "mvn clean package -DskipTests & mvn test"
 * </p>
 */
public abstract class AbstractLoadBundleTest {

    protected abstract FrameworkFactory getFactory();

    @Rule
    public OsgiRule osgi = new OsgiRule(getFactory());

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
        Assume.assumeTrue(
            "File does not exist: " + file.getAbsolutePath() + ". Run 'mvn package -DskipTests' before 'mvn test'",
            file.exists());
    }

    protected String getBundlePath() {
        return "target/" + bundleTestInfo.getArtifactId() + '-' + bundleTestInfo.getVersion() + ".jar";
    }

    /**
     * Gets the expected bundle symbolic name.
     *
     * @return the expected bundle symbolic name.
     */
    public String getExpectedBundleSymbolicName() {
        return "org.apache.logging." + bundleTestInfo.getArtifactId().replace('-', '.');
    }

    /**
     * Loads, starts, and stops a bundle.
     *
     * @throws BundleException
     */
    @Test
    public void testLoadStartStop() throws BundleException {
        final BundleContext bundleContext = osgi.getFramework().getBundleContext();
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
        bundle.uninstall();
        Assert.assertEquals("Bundle is not in UNINSTALLED state", Bundle.UNINSTALLED, bundle.getState());
    }

    /**
     * Tests the log of a simple message in an OSGi container
     */
    @Test
    public void testSimpleLogInAnOsgiContext() throws BundleException, ReflectiveOperationException {

        final BundleContext bundleContext = osgi.getFramework().getBundleContext();

        final Bundle api = bundleContext.installBundle("file:" + getBundlePath());
        final Bundle core = bundleContext.installBundle(
            "file:../log4j-core/target/log4j-core-" + bundleTestInfo.getVersion() + ".jar");
        final Bundle dummy = bundleContext.installBundle(
            "file:../log4j-samples/configuration/target/log4j-samples-configuration-" + bundleTestInfo.getVersion() + ".jar");

        api.start();
        core.start();
        dummy.start();

        final PrintStream bakStream = System.out;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final PrintStream logStream = new PrintStream(baos);
            System.setOut(logStream);

            log(dummy);

            final String result = baos.toString().substring(
                12).trim(); // remove the instant then the spaces at start and end, that are non constant
            Assert.assertEquals("[main] ERROR org.apache.logging.log4j.configuration.CustomConfiguration - Test OK",
                result);
        } finally {
            System.setOut(bakStream);
        }

        dummy.stop();
        core.stop();
        api.stop();

        dummy.uninstall();
        core.uninstall();
        api.uninstall();
    }

    /**
     * Tests LOG4J2-920.
     */
    @Test
    public void testMissingImportOfCoreOsgiPackage() throws BundleException, ReflectiveOperationException {

        final BundleContext bundleContext = osgi.getFramework().getBundleContext();

        final Bundle api = bundleContext.installBundle("file:" + getBundlePath());
        final Bundle core = bundleContext.installBundle(
            "file:../log4j-core/target/log4j-core-" + bundleTestInfo.getVersion() + ".jar");
        final Bundle dummy = bundleContext.installBundle(
            "file:../log4j-samples/configuration/target/log4j-samples-configuration-" + bundleTestInfo.getVersion() + ".jar");

        api.start();
        core.start();
        dummy.start();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream logStream = new PrintStream(baos);

        final PrintStream bakStream = setupStream(api, logStream);

        log(dummy);

        setupStream(api, bakStream);

        final boolean result = baos.toString().contains(
            "ERROR StatusLogger Unable to create context org.apache.logging.log4j.core.osgi.BundleContextSelector");
        Assert.assertFalse(
            "org.apache.logging.log4j.core.osgi;resolution:=optional is missing in Import-Package in the POM", result);

        dummy.stop();
        core.stop();
        api.stop();

        dummy.uninstall();
        core.uninstall();
        api.uninstall();
    }

    private void log(final Bundle dummy) throws ReflectiveOperationException {
        // use reflection to log in the context of the dummy bundle

        final Class<?> logManagerClass = dummy.loadClass("org.apache.logging.log4j.LogManager");
        final Method getLoggerMethod = logManagerClass.getMethod("getLogger", Class.class);

        final Class<?> loggerClass = dummy.loadClass("org.apache.logging.log4j.configuration.CustomConfiguration");

        final Object logger = getLoggerMethod.invoke(null, loggerClass);
        final Method infoMethod = logger.getClass().getMethod("error", Object.class);

        infoMethod.invoke(logger, "Test OK");
    }

    private PrintStream setupStream(final Bundle api, final PrintStream newStream) throws ReflectiveOperationException {
        // use reflection to access the classes internals and in the context of the api bundle

        final Class<?> statusLoggerClass = api.loadClass("org.apache.logging.log4j.status.StatusLogger");

        final Field statusLoggerField = statusLoggerClass.getDeclaredField("STATUS_LOGGER");
        statusLoggerField.setAccessible(true);
        final Object statusLoggerFieldValue = statusLoggerField.get(null);

        final Field loggerField = statusLoggerClass.getDeclaredField("logger");
        loggerField.setAccessible(true);
        final Object loggerFieldValue = loggerField.get(statusLoggerFieldValue);

        final Class<?> simpleLoggerClass = api.loadClass("org.apache.logging.log4j.simple.SimpleLogger");

        final Field streamField = simpleLoggerClass.getDeclaredField("stream");
        streamField.setAccessible(true);

        final PrintStream oldStream = (PrintStream) streamField.get(loggerFieldValue);

        streamField.set(loggerFieldValue, newStream);

        return oldStream;
    }

}
