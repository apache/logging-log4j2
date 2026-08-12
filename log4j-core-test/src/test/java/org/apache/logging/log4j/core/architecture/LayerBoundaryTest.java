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
package org.apache.logging.log4j.core.architecture;

import static org.apache.logging.log4j.core.test.architecture.ArchitectureTestSupport.assertViolationCountWithinBaseline;
import static org.apache.logging.log4j.core.test.architecture.ArchitectureTestSupport.collectLayerViolations;
import static org.apache.logging.log4j.core.test.architecture.ArchitectureTestSupport.importCoreProductionClasses;

import org.apache.logging.log4j.core.test.architecture.ArchitectureTestSupport;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Enforces util/config layer boundaries within {@code log4j-core}.
 *
 * <p>The util layer ({@code org.apache.logging.log4j.core.util..}) must remain independent of the
 * config layer ({@code org.apache.logging.log4j.core.config..}). Dependencies from util to config
 * represent upward layer violations that the SPI extraction work (EPIC-08) aims to eliminate.
 *
 * <p>Baseline measured on 2026-08-12 against the current {@code feat/forge-modernization-swarm}
 * branch. The threshold ratchets down as decoupling PRs land; do not increase it without explicit
 * architecture review.
 */
@AnalyzeClasses(
        packages = ArchitectureTestSupport.CORE_PACKAGE,
        importOptions = ImportOption.DoNotIncludeTests.class)
class LayerBoundaryTest {

    /**
     * Direct util-to-config dependency edges measured on 2026-08-12.
     *
     * <p>ForgeScore analysis estimated 1,214 cross-layer coupling instances across util and config
     * (both directions and broader coupling metrics). The measured util-to-config direct dependency
     * edge count ({@value #UTIL_TO_CONFIG_VIOLATION_BASELINE}) on this branch is the CI gate.
     * Reduce this constant as violations are remediated.
     */
    private static final int UTIL_TO_CONFIG_VIOLATION_BASELINE = 57;

    @Test
    void utilToConfigDependenciesDoNotExceedBaseline() {
        final JavaClasses classes = importCoreProductionClasses();
        final List<String> violations = collectLayerViolations(classes, ArchitectureTestSupport::isUtilLayerClass, ArchitectureTestSupport::isConfigLayerClass);
        assertViolationCountWithinBaseline("util layer must not depend on config layer", violations, UTIL_TO_CONFIG_VIOLATION_BASELINE);
    }

    @Test
    void utilToConfigViolationsIdentifyClassesAndPackages() {
        final JavaClasses classes = importCoreProductionClasses();
        final List<String> violations = collectLayerViolations(classes, ArchitectureTestSupport::isUtilLayerClass, ArchitectureTestSupport::isConfigLayerClass);
        Assertions.assertThat(violations)
                .allSatisfy(violation -> Assertions.assertThat(violation)
                        .contains(ArchitectureTestSupport.UTIL_LAYER_PREFIX)
                        .contains(ArchitectureTestSupport.CONFIG_LAYER_PREFIX));
    }
}
