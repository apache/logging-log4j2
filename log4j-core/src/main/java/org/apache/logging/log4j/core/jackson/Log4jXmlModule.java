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
package org.apache.logging.log4j.core.jackson;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import org.apache.logging.log4j.core.jackson.Initializers.SetupContextInitializer;
import org.apache.logging.log4j.core.jackson.Initializers.SimpleModuleInitializer;

/**
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 */
final class Log4jXmlModule extends JacksonXmlModule {

    private static final long serialVersionUID = 1L;
    private final boolean includeStacktrace;
    private final boolean stacktraceAsString;

    Log4jXmlModule(final boolean includeStacktrace, final boolean stacktraceAsString) {
        this.includeStacktrace = includeStacktrace;
        this.stacktraceAsString = stacktraceAsString;
        // MUST init here.
        // Calling this from setupModule is too late!
        new SimpleModuleInitializer().initialize(this, false);
    }

    @Override
    public void setupModule(final SetupContext context) {
        // Calling super is a MUST!
        super.setupModule(context);
        new SetupContextInitializer().setupModule(context, includeStacktrace, stacktraceAsString);
    }
}
