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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static org.apache.log4j.architecture.BridgeArchitectureTestSupport.ADAPTERS_WITHOUT_WRAPPER;
import static org.apache.log4j.architecture.BridgeArchitectureTestSupport.BRIDGE_PACKAGE;
import static org.apache.log4j.architecture.BridgeArchitectureTestSupport.BUILDERS_PACKAGE;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit fitness functions for Log4j 1.x bridge semantic naming consistency.
 *
 * <p>Enforces the {@code Adapter}/{@code Wrapper}/{@code Builder} naming conventions used across
 * {@code org.apache.log4j.bridge} and {@code org.apache.log4j.builders}. Pairing rules use
 * {@link com.tngtech.archunit.library.freeze.FreezingArchRule} with explicit allowlists so the
 * current codebase passes while blocking new inconsistencies.
 */
@AnalyzeClasses(packages = "org.apache.log4j", importOptions = ImportOption.DoNotIncludeTests.class)
class BridgeSemanticNamingConsistencyTest {

    @ArchTest
    static final ArchRule bridge_classes_use_adapter_or_wrapper_suffix = classes()
            .that()
            .resideInAPackage(BRIDGE_PACKAGE)
            .and()
            .areTopLevelClasses()
            .should()
            .haveSimpleNameEndingWith("Adapter")
            .orShould()
            .haveSimpleNameEndingWith("Wrapper")
            .because("bridge types must declare their adaptation direction via Adapter or Wrapper suffix");

    @ArchTest
    static final ArchRule concrete_builders_use_builder_suffix = classes()
            .that()
            .resideInAPackage(BUILDERS_PACKAGE)
            .and()
            .areTopLevelClasses()
            .and()
            .haveSimpleNameNotEndingWith("Holder")
            .and()
            .implement(org.apache.log4j.builders.Builder.class)
            .should()
            .haveSimpleNameEndingWith("Builder")
            .because("Log4j 1.x component builders must end with the Builder suffix");

    @ArchTest
    static final ArchRule bridge_adapters_and_wrappers_reside_in_bridge_package = freeze(classes()
            .that()
            .areTopLevelClasses()
            .and()
            .haveSimpleNameEndingWith("Adapter")
            .or()
            .haveSimpleNameEndingWith("Wrapper")
            .and()
            .resideInAPackage("org.apache.log4j..")
            .should()
            .resideInAPackage(BRIDGE_PACKAGE)
            .because("top-level Adapter and Wrapper bridge types must live in org.apache.log4j.bridge"));

    @Test
    void adaptersHaveWrapperCounterpartOrAllowlistEntry() {
        final JavaClasses importedClasses = BridgeArchitectureTestSupport.importBridgeProductionClasses();
        final ArchCondition<JavaClass> haveWrapperCounterpart =
                new ArchCondition<JavaClass>("have matching Wrapper type") {
            @Override
            public void check(final JavaClass adapter, final ConditionEvents events) {
                if (!adapter.getSimpleName().endsWith("Adapter")) {
                    return;
                }
                if (ADAPTERS_WITHOUT_WRAPPER.contains(adapter.getFullName())) {
                    return;
                }
                final String wrapperSimpleName =
                        adapter.getSimpleName().substring(0, adapter.getSimpleName().length() - "Adapter".length())
                                + "Wrapper";
                final boolean wrapperPresent = importedClasses.stream()
                        .anyMatch(clazz ->
                                clazz.getPackageName().equals(BRIDGE_PACKAGE)
                                        && clazz.getSimpleName().equals(wrapperSimpleName));
                if (!wrapperPresent) {
                    events.add(SimpleConditionEvent.violated(
                            adapter,
                            adapter.getFullName() + " has no counterpart " + BRIDGE_PACKAGE + "." + wrapperSimpleName));
                }
            }
        };

        freeze(classes()
                        .that()
                        .resideInAPackage(BRIDGE_PACKAGE)
                        .and()
                        .areTopLevelClasses()
                        .and()
                        .haveSimpleNameEndingWith("Adapter")
                        .should(haveWrapperCounterpart))
                .check(importedClasses);
    }
}
