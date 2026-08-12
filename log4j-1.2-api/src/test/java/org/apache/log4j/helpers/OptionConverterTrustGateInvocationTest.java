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
package org.apache.log4j.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.trustgate.spi.InputSanitizer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Bridge ArchUnit test for {@link OptionConverter} TrustGate wiring.
 *
 * <p>{@code log4j-1.2-api} bytecode is not on the {@code log4j-core-test} classpath, so the
 * {@code OptionConverter.substVars} invocation checks live here rather than in
 * {@code TrustGateInvocationTest}.
 */
class OptionConverterTrustGateInvocationTest {

    private static JavaClasses converterClasses;

    @BeforeAll
    static void importClasses() {
        converterClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importClasses(OptionConverter.class);
    }

    @Test
    void substVarsInvokesInputSanitizerValidate() {
        final JavaMethod publicSubstVars =
                converterClasses.get(OptionConverter.class).getMethod("substVars", String.class, Properties.class);
        assertCallsMethod(publicSubstVars, OptionConverter.class, "substVars", String.class, Properties.class, List.class);

        final JavaMethod privateSubstVars = converterClasses
                .get(OptionConverter.class)
                .getMethod("substVars", String.class, Properties.class, List.class);
        assertCallsMethod(privateSubstVars, OptionConverter.class, "isValidLookupExpression", String.class);

        final JavaMethod isValidLookupExpression =
                converterClasses.get(OptionConverter.class).getMethod("isValidLookupExpression", String.class);
        assertCallsInputSanitizerValidate(isValidLookupExpression);
    }

    private static void assertCallsInputSanitizerValidate(final JavaMethod method) {
        assertThat(method.getMethodCallsFromSelf())
                .as("%s should call InputSanitizer.validate()", method.getFullName())
                .anyMatch(OptionConverterTrustGateInvocationTest::isInputSanitizerValidateCall);
    }

    private static void assertCallsMethod(final JavaMethod caller, final Class<?> owner, final String name) {
        assertThat(caller.getMethodCallsFromSelf())
                .as("%s should call %s.%s()", caller.getFullName(), owner.getSimpleName(), name)
                .anyMatch(call -> call.getTargetOwner().isEquivalentTo(owner) && call.getName().equals(name));
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
        return call.getTargetOwner().isAssignableTo(InputSanitizer.class) && call.getName().equals("validate");
    }
}
