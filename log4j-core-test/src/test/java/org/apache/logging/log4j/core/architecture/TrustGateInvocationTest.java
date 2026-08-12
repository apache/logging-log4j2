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

import static org.apache.logging.log4j.core.test.architecture.ArchitectureTestSupport.importCoreProductionClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import java.net.URI;
import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.net.JndiManager;
import org.apache.logging.log4j.core.test.architecture.ArchitectureTestSupport;
import org.apache.logging.log4j.trustgate.spi.InputSanitizer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit fitness functions ensuring TrustGate {@link InputSanitizer#validate} is wired into core
 * trust-boundary entry points.
 *
 * <p>{@code Log4j1ConfigurationParser} lives in {@code log4j-1.2-api}, which is not on this
 * module's classpath; see {@code Log4j1ConfigurationParserTrustGateInvocationTest} in that module.
 */
@AnalyzeClasses(packages = ArchitectureTestSupport.CORE_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class TrustGateInvocationTest {

    private static JavaClasses coreClasses;

    @BeforeAll
    static void importClasses() {
        coreClasses = importCoreProductionClasses();
    }

    @Test
    void jndiManagerLookupInvokesInputSanitizerValidate() {
        final JavaMethod lookup = coreClasses.get(JndiManager.class).getMethod("lookup", String.class);
        assertCallsMethod(lookup, JndiManager.class, "validateJndiName", String.class);

        final JavaMethod validateJndiName =
                coreClasses.get(JndiManager.class).getMethod("validateJndiName", String.class);
        assertCallsInputSanitizerValidate(validateJndiName);
    }

    @Test
    void configurationFactoryUriPathInvokesInputSanitizerValidate() {
        final JavaMethod validateUri =
                coreClasses.get(ConfigurationFactory.class).getMethod("validateConfigurationUri", URI.class);
        assertCallsInputSanitizerValidate(validateUri);
    }

    @Test
    void strSubstitutorSubstituteInvokesInputSanitizerValidate() {
        final JavaMethod substitute = coreClasses
                .get(StrSubstitutor.class)
                .getMethod(
                        "substitute",
                        LogEvent.class,
                        StringBuilder.class,
                        int.class,
                        int.class,
                        List.class,
                        boolean.class);
        assertCallsMethod(substitute, StrSubstitutor.class, "validateLookupPattern", String.class);

        final JavaMethod validateLookupPattern =
                coreClasses.get(StrSubstitutor.class).getMethod("validateLookupPattern", String.class);
        assertCallsInputSanitizerValidate(validateLookupPattern);
    }

    private static void assertCallsInputSanitizerValidate(final JavaMethod method) {
        assertThat(method.getMethodCallsFromSelf())
                .as("%s should call InputSanitizer.validate()", method.getFullName())
                .anyMatch(TrustGateInvocationTest::isInputSanitizerValidateCall);
    }

    private static void assertCallsMethod(
            final JavaMethod caller, final Class<?> owner, final String name, final Class<?>... paramTypes) {
        assertThat(caller.getMethodCallsFromSelf())
                .as("%s should call %s.%s()", caller.getFullName(), owner.getSimpleName(), name)
                .anyMatch(call -> call.getTargetOwner().isEquivalentTo(owner)
                        && call.getName().equals(name)
                        && call.getTarget().getParameterTypes().size() == paramTypes.length);
    }

    private static boolean isInputSanitizerValidateCall(final JavaMethodCall call) {
        return call.getTargetOwner().isAssignableTo(InputSanitizer.class)
                && call.getName().equals("validate");
    }
}
