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
package org.apache.logging.log4j.osgi.tests;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.osgi.tests.junit.BundleTestInfo;
import org.apache.logging.log4j.osgi.tests.junit.OsgiRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Tests a basic Log4J 'setup' in an OSGi container.
 */
public abstract class AbstractLoadBundleTest {

    private BundleContext bundleContext;

    private final BundleTestInfo bundleTestInfo;

    private Path here;
    
    @Rule
    public OsgiRule osgi = new OsgiRule(getFactory());
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
    public void before() throws BundleException {
        bundleContext = osgi.getFramework().getBundleContext();
        
        here = Paths.get(".").toAbsolutePath().normalize();
    }

    private Bundle getApiBundle() throws BundleException {
        final Path apiPath = here.resolveSibling("log4j-api").resolve("target").resolve("log4j-api-" + bundleTestInfo.getVersion() + ".jar");
        return bundleContext.installBundle(apiPath.toUri().toString());
    }


    private Bundle getCoreBundle() throws BundleException {
        final Path corePath = here.resolveSibling("log4j-core").resolve("target").resolve("log4j-core-" + bundleTestInfo.getVersion() + ".jar");
        return bundleContext.installBundle(corePath.toUri().toString());
    }
    
    private Bundle getDummyBundle() throws BundleException {
        final Path dumyPath = here.resolveSibling("log4j-samples").resolve("configuration").resolve("target").resolve("log4j-samples-configuration-" + bundleTestInfo.getVersion() + ".jar");
        return bundleContext.installBundle(dumyPath.toUri().toString());
    }

    protected abstract FrameworkFactory getFactory();
    
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

    private void start(final Bundle api, final Bundle core, final Bundle dummy) throws BundleException {
        api.start();
        core.start();
        dummy.start();        
    }

    private void stop(final Bundle api, final Bundle core, final Bundle dummy) throws BundleException {
        dummy.stop();
        core.stop();
        api.stop();
    }
    /**
     * Tests LOG4J2-1637.
     */
    @Test
    public void testClassNotFoundErrorLogger() throws BundleException {

        final Bundle api = getApiBundle();
        final Bundle core = getCoreBundle();

        api.start();
        // fails if LOG4J2-1637 is not fixed
        try {
            core.start();
        }
        catch (final BundleException ex) {
            final Throwable t = ex.getCause();
            if (t != null)
            {
                final Throwable t2 = t.getCause();
                if (t2 != null)
                {
                    final String cause = t2.toString();
                    final boolean result = cause.equals("java.lang.ClassNotFoundException: org.apache.logging.log4j.Logger") // Equinox
                                  || cause.equals("java.lang.ClassNotFoundException: org.apache.logging.log4j.Logger not found by org.apache.logging.log4j.core [2]"); // Felix
                    Assert.assertFalse("org.apache.logging.log4j package is not properly imported in org.apache.logging.log4j.core bundle, check that the package is exported from api and is not split between api and core", result);
                }
                throw ex; // rethrow if the cause of the exception is something else
            }
        }

        core.stop();
        api.stop();
        
        core.uninstall();
        api.uninstall();
    }

    /**
     * Tests LOG4J2-920.
     */
    @Test
    @Ignore("org.osgi.framework.BundleException: The bundle \"org.apache.logging.log4j.core_2.7.1.SNAPSHOT [2]\" could not be resolved. Reason: Missing Constraint: Import-Package: javax.jms; version=\"[1.1.0,2.0.0)\"")
    public void testMissingImportOfCoreOsgiPackage() throws BundleException, ReflectiveOperationException {

        final Bundle api = getApiBundle();
        final Bundle core = getCoreBundle();
        final Bundle dummy = getDummyBundle();

        start(api, core, dummy);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream logStream = new PrintStream(baos);

        final PrintStream bakStream = setupStream(api, logStream);

        log(dummy);

        setupStream(api, bakStream);

        final boolean result = baos.toString().contains(
            "ERROR StatusLogger Unable to create context org.apache.logging.log4j.core.osgi.BundleContextSelector");
        Assert.assertFalse(
            "org.apache.logging.log4j.core.osgi;resolution:=optional is missing in Import-Package in the POM", result);

        stop(api, core, dummy);
        uninstall(api, core, dummy);
    }

    /**
     * Tests the log of a simple message in an OSGi container
     */
    @Test
    @Ignore("org.osgi.framework.BundleException: The bundle \"org.apache.logging.log4j.core_2.7.1.SNAPSHOT [2]\" could not be resolved. Reason: Missing Constraint: Import-Package: javax.jms; version=\"[1.1.0,2.0.0)\"")
    public void testSimpleLogInAnOsgiContext() throws BundleException, ReflectiveOperationException {

        final Bundle api = getApiBundle();
        final Bundle core = getCoreBundle();
        final Bundle dummy = getDummyBundle();

        start(api, core, dummy);

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

        stop(api, core, dummy);
        uninstall(api, core, dummy);
    }

    private void uninstall(final Bundle api, final Bundle core, final Bundle dummy) throws BundleException {
        dummy.uninstall();
        core.uninstall();
        api.uninstall();
    }
}
