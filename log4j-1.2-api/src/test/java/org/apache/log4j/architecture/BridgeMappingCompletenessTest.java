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

import static org.apache.log4j.architecture.BridgeArchitectureTestSupport.BRIDGE_PACKAGE;
import static org.apache.log4j.architecture.BridgeArchitectureTestSupport.BUILDERS_PACKAGE;
import static org.apache.log4j.architecture.BridgeArchitectureTestSupport.UNMAPPED_BRIDGE_CLASSES;
import static org.apache.log4j.architecture.BridgeArchitectureTestSupport.UNMAPPED_BUILDER_CLASSES;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit fitness functions ensuring bridge implementation completeness against
 * {@code api-mapping-1x-to-2x.json}.
 *
 * <p>Primary {@code bridgeClass} entries from the mapping must exist on the classpath and follow
 * builder naming conventions. Bridge and builder types outside the mapping must be explicitly
 * allowlisted until the reference is extended.
 */
@AnalyzeClasses(packages = "org.apache.log4j", importOptions = ImportOption.DoNotIncludeTests.class)
class BridgeMappingCompletenessTest {

    private static List<Map<String, Object>> mappingEntries;
    private static Set<String> mappedBridgeClasses;
    private static JavaClasses productionClasses;

    @BeforeAll
    static void loadMapping() throws IOException {
        mappingEntries = BridgeArchitectureTestSupport.loadMappingEntries();
        mappedBridgeClasses = BridgeArchitectureTestSupport.mappedBridgeClasses(mappingEntries);
        productionClasses = BridgeArchitectureTestSupport.importBridgeProductionClasses();
    }

    @Test
    void mappingBridgeClassesExistOnClasspath() {
        for (final String bridgeClass : mappedBridgeClasses) {
            assertTrue(
                    productionClasses.stream().anyMatch(clazz -> clazz.getFullName().equals(bridgeClass)),
                    "mapping bridgeClass must exist: " + bridgeClass);
        }
    }

    @Test
    void mappingBuilderBridgeClassesFollowSourceNamingConvention() {
        for (final Map<String, Object> entry : mappingEntries) {
            final String bridgeClass = (String) entry.get("bridgeClass");
            if (!bridgeClass.startsWith(BUILDERS_PACKAGE)) {
                continue;
            }
            final String sourceClass = (String) entry.get("sourceClass");
            final String expectedSimpleName = simpleName(sourceClass) + "Builder";
            assertTrue(
                    bridgeClass.endsWith("." + expectedSimpleName),
                    "builder bridgeClass "
                            + bridgeClass
                            + " should be named "
                            + expectedSimpleName
                            + " for source "
                            + sourceClass);
        }
    }

    @Test
    void unmappedBridgeClassesAreAllowlisted() {
        final Set<String> bridgeClasses = productionClasses.stream()
                .filter(clazz -> clazz.getPackageName().equals(BRIDGE_PACKAGE))
                .filter(JavaClass::isTopLevelClass)
                .map(JavaClass::getFullName)
                .collect(Collectors.toSet());

        final Set<String> undocumented = new HashSet<>(bridgeClasses);
        undocumented.removeAll(mappedBridgeClasses);
        undocumented.removeAll(UNMAPPED_BRIDGE_CLASSES);

        assertTrue(
                undocumented.isEmpty(),
                "bridge classes must appear in api-mapping or UNMAPPED_BRIDGE_CLASSES allowlist: " + undocumented);
    }

    @Test
    void unmappedConcreteBuildersAreAllowlisted() {
        final Set<String> builderClasses = productionClasses.stream()
                .filter(clazz -> clazz.getPackageName().startsWith(BUILDERS_PACKAGE))
                .filter(JavaClass::isTopLevelClass)
                .filter(clazz -> clazz.getSimpleName().endsWith("Builder"))
                .filter(clazz -> clazz.isAssignableTo(org.apache.log4j.builders.Builder.class))
                .filter(clazz -> !clazz.isInterface())
                .filter(clazz -> !clazz.getModifiers().contains(JavaModifier.ABSTRACT))
                .map(JavaClass::getFullName)
                .collect(Collectors.toSet());

        final Set<String> undocumented = new HashSet<>(builderClasses);
        undocumented.removeAll(mappedBridgeClasses);
        undocumented.removeAll(UNMAPPED_BUILDER_CLASSES);

        assertTrue(
                undocumented.isEmpty(),
                "builder classes must appear in api-mapping or UNMAPPED_BUILDER_CLASSES allowlist: " + undocumented);
    }

    @Test
    void mappedBridgeClassesNotInAllowlists() {
        assertFalse(
                mappedBridgeClasses.stream().anyMatch(UNMAPPED_BRIDGE_CLASSES::contains),
                "UNMAPPED_BRIDGE_CLASSES must not overlap mapped bridgeClass entries");
        assertFalse(
                mappedBridgeClasses.stream().anyMatch(UNMAPPED_BUILDER_CLASSES::contains),
                "UNMAPPED_BUILDER_CLASSES must not overlap mapped bridgeClass entries");
    }

    @Test
    void unmappedBridgeAndBuilderClassesAreFrozen() {
        final ArchCondition<JavaClass> isDocumentedInMappingOrAllowlist =
                new ArchCondition<JavaClass>("be documented in api-mapping or explicit allowlist") {
            @Override
            public void check(final JavaClass clazz, final ConditionEvents events) {
                if (mappedBridgeClasses.contains(clazz.getFullName())) {
                    return;
                }
                if (UNMAPPED_BRIDGE_CLASSES.contains(clazz.getFullName())
                        || UNMAPPED_BUILDER_CLASSES.contains(clazz.getFullName())) {
                    return;
                }
                events.add(SimpleConditionEvent.violated(
                        clazz, clazz.getFullName() + " is not covered by api-mapping or allowlist"));
            }
        };

        freeze(classes()
                        .that()
                        .resideInAnyPackage(BRIDGE_PACKAGE, BUILDERS_PACKAGE + "..")
                        .and()
                        .areTopLevelClasses()
                        .and()
                        .haveSimpleNameEndingWith("Adapter")
                        .or()
                        .haveSimpleNameEndingWith("Wrapper")
                        .or()
                        .haveSimpleNameEndingWith("Builder")
                        .and()
                        .areNotInterfaces()
                        .and()
                        .doNotHaveModifier(JavaModifier.ABSTRACT)
                        .should(isDocumentedInMappingOrAllowlist))
                .check(productionClasses);
    }

    private static String simpleName(final String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }
}
