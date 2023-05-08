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
package org.apache.logging.log4j.core.test.junit;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

final class Log4jEventRecorderAnchor {

    private Log4jEventRecorderAnchor() {}

    static Log4jEventRecorder recorder(
            final ExtensionContext extensionContext,
            final ParameterContext parameterContext) {
        return recorderByParameterName(extensionContext).computeIfAbsent(
                parameterContext.getParameter().getName(),
                ignored -> new Log4jEventRecorder());
    }

    static Collection<Log4jEventRecorder> recorders(final ExtensionContext extensionContext) {
        return recorderByParameterName(extensionContext).values();
    }

    private static Map<String, Log4jEventRecorder> recorderByParameterName(final ExtensionContext extensionContext) {
        ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(
                Log4jEventRecorder.class,
                extensionContext.getRequiredTestClass(),
                extensionContext.getRequiredTestMethod());
        final ExtensionContext.Store store = extensionContext.getStore(namespace);
        @SuppressWarnings("unchecked")
        final Map<String, Log4jEventRecorder> recorderByParameterName = store.getOrComputeIfAbsent(
                "recorderByParameterName",
                ignored -> new LinkedHashMap<>(),
                Map.class);
        return recorderByParameterName;
    }

}
