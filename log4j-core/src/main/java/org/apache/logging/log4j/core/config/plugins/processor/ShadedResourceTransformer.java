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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.codehaus.plexus.util.IOUtil;

/**
 */
public final class ShadedResourceTransformer implements ResourceTransformer {

    private final PluginCache pluginCache = new PluginCache();
    private final String outputResource;

    public ShadedResourceTransformer() {
        this.outputResource = PluginProcessor.PLUGIN_CACHE_FILE;
    }

    ShadedResourceTransformer(final String outputResource) {
        this.outputResource = outputResource;
    }

    @Override
    public boolean canTransformResource(final String resource) {
        return resource.startsWith(PluginProcessor.PLUGIN_CACHE_FILE);
    }

    @Override
    public void processResource(final String resource, final InputStream in, final List<Relocator> list) throws IOException {
        assert PluginProcessor.PLUGIN_CACHE_FILE.equals(resource);
        pluginCache.loadCacheFile(in);
    }

    @Override
    public boolean hasTransformedResource() {
        return 0 < pluginCache.size();
    }

    @Override
    public void modifyOutputStream(final JarOutputStream stream) throws IOException {
        ByteArrayOutputStream copy = new ByteArrayOutputStream();
        pluginCache.writeCache(copy);
        stream.putNextEntry(new JarEntry(outputResource));
        IOUtil.copy(new ByteArrayInputStream(copy.toByteArray()), stream);
    }

}
