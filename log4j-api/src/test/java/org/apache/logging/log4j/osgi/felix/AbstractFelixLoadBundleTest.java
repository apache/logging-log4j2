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
package org.apache.logging.log4j.osgi.felix;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.osgi.AbstractLoadBundleTest;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Tests loading the Core bundle into an Apache Felix OSGi container.
 * <p>
 * Requires that "mvn package" has been previously run, otherwise test fails its JUnit {@link Assume}.
 * </p>
 * <p>
 * For example, on Windows: "mvn clean package -DskipTests & mvn test"
 * </p>
 */
public abstract class AbstractFelixLoadBundleTest extends AbstractLoadBundleTest {

    /**
     * Initializes the OSGi framework.
     * 
     * @throws BundleException
     */
    @BeforeClass
    public static void initOsgiFramework() throws BundleException {
        final FrameworkFactory factory = new org.apache.felix.framework.FrameworkFactory();
        final Map<String, String> configMap = new HashMap<String, String>(2);
        // Cleans framework before first init. Subsequent init invocations do not clean framework.
        configMap.put("org.osgi.framework.storage.clean", "onFirstInit");
        // Delegates loading of endorsed libraries to JVM classloader
        // config.put("org.osgi.framework.bootdelegation", "javax.*,org.w3c.*,org.xml.*");
        OsgiFramework = factory.newFramework(configMap);
        OsgiFramework.init();
        OsgiFramework.start();
    }

}
