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
package org.apache.logging.log4j.core.jackson;

import org.apache.logging.log4j.core.jackson.Initializers.SetupContextInitializer;
import org.apache.logging.log4j.core.jackson.Initializers.SetupContextJsonInitializer;
import org.apache.logging.log4j.core.jackson.Initializers.SimpleModuleInitializer;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 */
final class Log4jYamlModule extends SimpleModule {

    private static final long serialVersionUID = 1L;
    private final boolean encodeThreadContextAsList;
    private final boolean includeStacktrace;

    Log4jYamlModule(final boolean encodeThreadContextAsList, final boolean includeStacktrace) {
        super(Log4jYamlModule.class.getName(), new Version(2, 0, 0, null, null, null));
        this.encodeThreadContextAsList = encodeThreadContextAsList;
        this.includeStacktrace = includeStacktrace;
        // MUST init here.
        // Calling this from setupModule is too late!
        //noinspection ThisEscapedInObjectConstruction
        new SimpleModuleInitializer().initialize(this);
    }

    @Override
    public void setupModule(final SetupContext context) {
        // Calling super is a MUST!
        super.setupModule(context);
        if (encodeThreadContextAsList) {
            new SetupContextInitializer().setupModule(context, includeStacktrace);
        } else {
            new SetupContextJsonInitializer().setupModule(context, includeStacktrace);
        }
    }
}
