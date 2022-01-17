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

package org.apache.logging.log4j.plugins.util;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.di.model.PluginModule;
import org.apache.logging.log4j.plugins.di.model.PluginSource;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class PluginLoader {
    private static final Logger LOGGER = StatusLogger.getLogger();

    public static List<PluginSource> loadPluginSourcesFromMainClassLoader() {
        final List<PluginSource> sources = new ArrayList<>();
        for (final ClassLoader classLoader : LoaderUtil.getClassLoaders()) {
            sources.addAll(loadPluginSources(classLoader));
        }
        return sources;
    }

    public static List<PluginSource> loadPluginSources(final ClassLoader classLoader) {
        final long startTime = System.nanoTime();
        final ServiceLoader<PluginModule> serviceLoader = ServiceLoader.load(PluginModule.class, classLoader);
        final List<PluginSource> sources = new ArrayList<>();
        for (final PluginModule module : serviceLoader) {
            sources.addAll(module.getPluginSources());
        }
        final int numPlugins = sources.size();
        LOGGER.debug(() -> {
            final long endTime = System.nanoTime();
            final DecimalFormat numFormat = new DecimalFormat("#0.000000");
            return "Took " + numFormat.format((endTime - startTime) * 1e-9) +
                    " seconds to load " + numPlugins +
                    " plugins from " + classLoader;
        });
        return sources;
    }

}
