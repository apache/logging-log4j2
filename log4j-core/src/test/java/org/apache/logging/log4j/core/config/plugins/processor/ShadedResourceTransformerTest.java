/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.junit.Test;

import static java.util.Collections.EMPTY_LIST;;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public final class ShadedResourceTransformerTest {

    @Test
    public void testCanTransformResource() {
        final ShadedResourceTransformer transformer = new ShadedResourceTransformer();
        assertFalse(transformer.canTransformResource("META-INF/MANIFEST.MF"));
        assertTrue(transformer.canTransformResource(PluginProcessor.PLUGIN_CACHE_FILE));
    }

    @Test
    public void testTransformResource() throws IOException {

        final String outputResource = "META-INF/Log4j2Plugins-test-output.dat";
        final ShadedResourceTransformer transformer = new ShadedResourceTransformer(outputResource);
        final PluginCache pluginCache = new PluginCache();
        int minCategories = 0;

        // find and load test resources
        final List<URL> resources = new ArrayList<URL>();
        ClassLoader cl = ShadedResourceTransformerTest.class.getClassLoader();
        minCategories = load(cl.getResources("META-INF/Log4j2Plugins-test-0.dat"), resources, pluginCache, minCategories);
        minCategories = load(cl.getResources("META-INF/Log4j2Plugins-test-1.dat"), resources, pluginCache, minCategories);

        // loop through processing all resources
        for(URL url : resources) {
            transformer.processResource(PluginProcessor.PLUGIN_CACHE_FILE, url.openStream(), EMPTY_LIST);
        }

        // write transformed resource to temp jar file
        assertTrue(transformer.hasTransformedResource());
        JarOutputStream jos = new JarOutputStream(new FileOutputStream("target/test-shaded.jar"));
        transformer.modifyOutputStream(jos);
        jos.closeEntry();
        jos.close();

        // read transformed resource and verify
        JarFile jarFile = new JarFile("target/test-shaded.jar");
        pluginCache.loadCacheFile(jarFile.getInputStream(jarFile.getJarEntry(outputResource)));
        assertTrue(minCategories <= pluginCache.size());
    }

    private static int load(
            final Enumeration<URL> urls,
            final List<URL> resources,
            final PluginCache pluginCache,
            final int minCategories) throws IOException {

        assertTrue(urls.hasMoreElements());
        resources.add(urls.nextElement());
        assertFalse(urls.hasMoreElements());
        pluginCache.loadCacheFiles(urls);
        return Math.max(minCategories, pluginCache.size());
    }

}
