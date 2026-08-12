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
package org.apache.log4j.architecture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared helpers for ArchUnit bridge architecture tests in {@code log4j-1.2-api}.
 */
public final class BridgeArchitectureTestSupport {

    public static final String BRIDGE_PACKAGE = "org.apache.log4j.bridge";
    public static final String BUILDERS_PACKAGE = "org.apache.log4j.builders";
    public static final String MAPPING_RESOURCE = "api-mapping-1x-to-2x.json";

    /**
     * Bridge infrastructure referenced from mapped types but not listed as a primary {@code bridgeClass}.
     */
    public static final Set<String> UNMAPPED_BRIDGE_CLASSES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "org.apache.log4j.bridge.AppenderWrapper",
            "org.apache.log4j.bridge.ErrorHandlerAdapter",
            "org.apache.log4j.bridge.FilterAdapter",
            "org.apache.log4j.bridge.FilterWrapper",
            "org.apache.log4j.bridge.LayoutWrapper",
            "org.apache.log4j.bridge.LogEventAdapter",
            "org.apache.log4j.bridge.LogEventWrapper",
            "org.apache.log4j.bridge.RewritePolicyAdapter",
            "org.apache.log4j.bridge.RewritePolicyWrapper")));

    /**
     * Internal builders not yet covered by the public API mapping reference (WO-043).
     */
    public static final Set<String> UNMAPPED_BUILDER_CLASSES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "org.apache.log4j.builders.appender.EnhancedRollingFileAppenderBuilder",
            "org.apache.log4j.builders.appender.NullAppenderBuilder",
            "org.apache.log4j.builders.appender.RewriteAppenderBuilder",
            "org.apache.log4j.builders.filter.DenyAllFilterBuilder",
            "org.apache.log4j.builders.filter.LevelMatchFilterBuilder",
            "org.apache.log4j.builders.filter.LevelRangeFilterBuilder",
            "org.apache.log4j.builders.filter.StringMatchFilterBuilder",
            "org.apache.log4j.builders.layout.HtmlLayoutBuilder",
            "org.apache.log4j.builders.layout.SimpleLayoutBuilder",
            "org.apache.log4j.builders.layout.TTCCLayoutBuilder",
            "org.apache.log4j.builders.layout.XmlLayoutBuilder",
            "org.apache.log4j.builders.rolling.CompositeTriggeringPolicyBuilder",
            "org.apache.log4j.builders.rolling.SizeBasedTriggeringPolicyBuilder",
            "org.apache.log4j.builders.rolling.TimeBasedRollingPolicyBuilder")));

    /**
     * Adapters that intentionally have no {@code *Wrapper} counterpart in the bridge.
     */
    public static final Set<String> ADAPTERS_WITHOUT_WRAPPER = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "org.apache.log4j.bridge.ErrorHandlerAdapter", "org.apache.log4j.bridge.LogEventAdapter")));

    private BridgeArchitectureTestSupport() {}

    public static JavaClasses importBridgeProductionClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.apache.log4j");
    }

    public static List<Map<String, Object>> loadMappingEntries() throws IOException {
        try (InputStream inputStream =
                BridgeArchitectureTestSupport.class.getClassLoader().getResourceAsStream(MAPPING_RESOURCE)) {
            if (inputStream == null) {
                throw new IOException("missing classpath resource: " + MAPPING_RESOURCE);
            }
            return new ObjectMapper().readValue(inputStream, new TypeReference<List<Map<String, Object>>>() {});
        }
    }

    public static Set<String> mappedBridgeClasses(final List<Map<String, Object>> entries) {
        return entries.stream().map(entry -> (String) entry.get("bridgeClass")).collect(Collectors.toSet());
    }

    public static Set<String> mappedSourceClasses(final List<Map<String, Object>> entries) {
        return entries.stream().map(entry -> (String) entry.get("sourceClass")).collect(Collectors.toSet());
    }
}
