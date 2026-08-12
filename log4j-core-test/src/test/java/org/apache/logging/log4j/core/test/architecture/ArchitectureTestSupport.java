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
package org.apache.logging.log4j.core.test.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.assertj.core.api.Assertions;

/**
 * Shared helpers for ArchUnit architecture tests in {@code log4j-core}.
 */
public final class ArchitectureTestSupport {

    public static final String CORE_PACKAGE = "org.apache.logging.log4j.core";
    public static final String UTIL_LAYER_PREFIX = "org.apache.logging.log4j.core.util";
    public static final String CONFIG_LAYER_PREFIX = "org.apache.logging.log4j.core.config";
    public static final String SPI_LAYER_PREFIX = "org.apache.logging.log4j.config.spi";
    public static final String COMMON_LAYER_PREFIX = "org.apache.logging.log4j.common";

    private ArchitectureTestSupport() {}

    public static JavaClasses importCoreProductionClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(CORE_PACKAGE);
    }

    public static boolean isUtilLayerClass(final JavaClass clazz) {
        return clazz.getPackageName().startsWith(UTIL_LAYER_PREFIX);
    }

    public static boolean isConfigLayerClass(final JavaClass clazz) {
        return clazz.getPackageName().startsWith(CONFIG_LAYER_PREFIX);
    }

    public static boolean isSpiLayerClass(final JavaClass clazz) {
        return clazz.getPackageName().startsWith(SPI_LAYER_PREFIX);
    }

    public static boolean isCommonLayerClass(final JavaClass clazz) {
        return clazz.getPackageName().startsWith(COMMON_LAYER_PREFIX);
    }

    /**
     * Layered architecture rule for util/config decoupling within {@code log4j-core}.
     *
     * <p>Both util and config may depend on {@code config-spi} and {@code log4j-common} types
     * (outside the scanned {@code log4j-core} package tree). Direct util-to-config and
     * config-to-util edges within core are forbidden.
     */
    public static ArchRule utilAndConfigLayersAreIndependent() {
        return Architectures.layeredArchitecture()
                .consideringOnlyDependenciesInAnyPackage(UTIL_LAYER_PREFIX + "..", CONFIG_LAYER_PREFIX + "..")
                .layer("util")
                .definedBy(UTIL_LAYER_PREFIX + "..")
                .layer("config")
                .definedBy(CONFIG_LAYER_PREFIX + "..")
                .whereLayer("util")
                .mayNotAccessAnyLayer()
                .whereLayer("config")
                .mayNotAccessAnyLayer();
    }

    public static List<String> collectLayerRuleViolations(final JavaClasses classes, final ArchRule rule) {
        final com.tngtech.archunit.lang.EvaluationResult result = rule.evaluate(classes);
        final List<String> violations = new ArrayList<>();
        if (result.hasViolation()) {
            result.getFailureReport().getDetails().forEach(detail -> violations.add(detail.toString()));
        }
        return violations;
    }

    public static List<String> collectLayerViolations(
            final JavaClasses classes,
            final Predicate<JavaClass> sourceFilter,
            final Predicate<JavaClass> forbiddenTargetFilter) {
        return collectLayerViolations(classes, sourceFilter, forbiddenTargetFilter, target -> false);
    }

    public static List<String> collectLayerViolations(
            final JavaClasses classes,
            final Predicate<JavaClass> sourceFilter,
            final Predicate<JavaClass> forbiddenTargetFilter,
            final Predicate<JavaClass> allowedTargetFilter) {
        final List<String> violations = new ArrayList<>();
        for (final JavaClass source : classes) {
            if (!sourceFilter.test(source)) {
                continue;
            }
            for (final Dependency dependency : source.getDirectDependenciesFromSelf()) {
                final JavaClass target = dependency.getTargetClass();
                if (allowedTargetFilter.test(target)) {
                    continue;
                }
                if (forbiddenTargetFilter.test(target)) {
                    violations.add(source.getName() + " -> " + target.getName());
                }
            }
        }
        return violations;
    }

    public static void assertViolationCountWithinBaseline(
            final String ruleDescription, final List<String> violations, final int baseline) {
        Assertions.assertThat(violations)
                .as(
                        "%s (baseline=%d, actual=%d)%nViolations:%n  %s",
                        ruleDescription, baseline, violations.size(), String.join("\n  ", violations))
                .hasSizeLessThanOrEqualTo(baseline);
    }
}
