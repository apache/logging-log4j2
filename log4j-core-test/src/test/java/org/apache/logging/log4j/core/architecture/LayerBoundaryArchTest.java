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

import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static org.apache.logging.log4j.core.test.architecture.ArchitectureTestSupport.utilAndConfigLayersAreIndependent;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.apache.logging.log4j.core.test.architecture.ArchitectureTestSupport;

/**
 * Freezing ArchUnit fitness function for util/config layer boundaries in {@code log4j-core}.
 *
 * <p>Uses {@link com.tngtech.archunit.library.freeze.FreezingArchRule} to persist the current
 * util/config coupling baseline in {@code src/test/resources/archunit_store/}. CI fails when new
 * layer violations appear beyond the frozen set; update the store only when remediating existing
 * violations.
 */
@AnalyzeClasses(packages = ArchitectureTestSupport.CORE_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class LayerBoundaryArchTest {

    @ArchTest
    static final ArchRule util_and_config_layers_are_independent =
            freeze(utilAndConfigLayersAreIndependent()).as("util and config layers must remain independent");
}
