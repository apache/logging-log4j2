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
import static org.apache.logging.log4j.core.test.architecture.ArchitectureTestSupport.importCoreProductionClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.dependencies.SliceRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.core.test.architecture.ArchitectureTestSupport;
import org.junit.jupiter.api.Test;

/**
 * Detects package-level circular dependencies among top-level {@code log4j-core} packages.
 *
 * <p>Uses ArchUnit slice analysis on {@code org.apache.logging.log4j.core.(*)..} to surface
 * package cycles that block modularization. Cycles are tracked with a baseline threshold so CI
 * fails only when new cycles appear.
 */
@AnalyzeClasses(packages = ArchitectureTestSupport.CORE_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class CircularDependencyTest {

    /**
     * ArchUnit failure-report detail entries for package cycles, measured on 2026-08-12. The core
     * packages form a densely connected graph today; this baseline captures the current diagnostic
     * volume and blocks growth until cycles are broken apart.
     */
    private static final int PACKAGE_CYCLE_DETAIL_BASELINE = 100;

    private static final SliceRule SLICES_SHOULD_BE_FREE_OF_CYCLES = SlicesRuleDefinition.slices()
            .matching("org.apache.logging.log4j.core.(*)..")
            .should()
            .beFreeOfCycles();

    @Test
    void packageCyclesDoNotExceedBaseline() {
        final JavaClasses classes = importCoreProductionClasses();
        final EvaluationResult result = SLICES_SHOULD_BE_FREE_OF_CYCLES.evaluate(classes);
        final List<String> cycleDetails = new ArrayList<>();
        if (result.hasViolation()) {
            result.getFailureReport().getDetails().forEach(detail -> cycleDetails.add(detail.toString()));
        }
        assertViolationCountWithinBaseline(
                "top-level core package slices must not introduce new cycle diagnostics",
                cycleDetails,
                PACKAGE_CYCLE_DETAIL_BASELINE);
    }
}
