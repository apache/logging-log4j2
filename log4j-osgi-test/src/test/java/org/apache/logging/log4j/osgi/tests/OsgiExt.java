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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * JUnit rule to initialize and shutdown an OSGi framework.
 */
class OsgiExt implements AfterEachCallback, BeforeEachCallback {
    private final FrameworkFactory factory;
    private Framework framework;

    OsgiExt(final FrameworkFactory factory) {
        this.factory = factory;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (framework != null) {
            try {
                framework.stop();
            } catch (final BundleException e) {
                throw new RuntimeException(e);
            } finally {
                framework = null;
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        try (final InputStream is = OsgiExt.class.getResourceAsStream("/osgi.properties")) {
            final Properties props = new Properties();
            props.load(is);
            final Map<String, String> configMap = props.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> String.valueOf(e.getKey()),
                            e -> String.valueOf(e.getValue()),
                            (prev, next) -> next,
                            HashMap::new));
            framework = factory.newFramework(configMap);
            framework.init();
            framework.start();
        }
    }

    public Framework getFramework() {
        return framework;
    }

    @Override
    public String toString() {
        return "OsgiExt [factory=" + factory + ", framework=" + framework + "]";
    }
}
