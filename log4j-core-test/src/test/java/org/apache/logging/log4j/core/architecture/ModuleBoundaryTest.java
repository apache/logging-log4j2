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

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import java.util.List;
import org.apache.logging.log4j.core.test.architecture.ArchitectureTestSupport;
import org.junit.jupiter.api.Test;

/**
 * Verifies integration-oriented packages do not reach into util internals.
 *
 * <p>Integration appenders and network adapters (HTTP, JMS, JDBC, SMTP) should depend on public
 * util APIs rather than {@code org.apache.logging.log4j.core.util.internal..} implementation
 * details. A baseline threshold prevents new inappropriate couplings during modularization.
 */
@AnalyzeClasses(
        packages = ArchitectureTestSupport.CORE_PACKAGE,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryTest {

    /**
     * Integration packages importing util internals, measured on 2026-08-12.
     */
    private static final int INTEGRATION_TO_UTIL_INTERNAL_BASELINE = 1;

    private static final String[] INTEGRATION_PACKAGES = {
        "org.apache.logging.log4j.core.appender.mom",
        "org.apache.logging.log4j.core.appender.db",
        "org.apache.logging.log4j.core.net",
    };

    private static final String[] INTEGRATION_APPENDER_SIMPLE_NAMES = {"HttpAppender", "SmtpAppender", "JmsAppender"};

    private static final String UTIL_INTERNAL_MARKER = ".util.internal";

    @Test
    void integrationPackagesMustNotImportUtilInternalsWithinBaseline() {
        final JavaClasses classes = importCoreProductionClasses();
        final List<String> violations = collectLayerViolations(
                classes, ModuleBoundaryTest::isIntegrationClass, ModuleBoundaryTest::isUtilInternalClass);
        assertViolationCountWithinBaseline(
                "integration packages must not depend on util.internal", violations, INTEGRATION_TO_UTIL_INTERNAL_BASELINE);
    }

    private static boolean isIntegrationClass(final JavaClass clazz) {
        if (isNamedIntegrationAppender(clazz)) {
            return true;
        }
        final String packageName = clazz.getPackageName();
        for (final String integrationPackage : INTEGRATION_PACKAGES) {
            if (packageName.startsWith(integrationPackage)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNamedIntegrationAppender(final JavaClass clazz) {
        if (!clazz.getPackageName().startsWith("org.apache.logging.log4j.core.appender")) {
            return false;
        }
        final String simpleName = clazz.getSimpleName();
        for (final String name : INTEGRATION_APPENDER_SIMPLE_NAMES) {
            if (simpleName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUtilInternalClass(final JavaClass clazz) {
        return clazz.getPackageName().contains(UTIL_INTERNAL_MARKER);
    }
}
