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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * A Jackson {@link ObjectMapper} initialized for Log4j.
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 */
public class Log4jYamlObjectMapper extends YAMLMapper {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new instance using the {@link Log4jYamlModule}.
     */
    public Log4jYamlObjectMapper() {
        this(false, true, false);
    }

    /**
     * Create a new instance using the {@link Log4jYamlModule}.
     */
    public Log4jYamlObjectMapper(
            final boolean encodeThreadContextAsList,
            final boolean includeStacktrace,
            final boolean stacktraceAsString) {
        this.registerModule(new Log4jYamlModule(encodeThreadContextAsList, includeStacktrace, stacktraceAsString));
        this.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }
}
