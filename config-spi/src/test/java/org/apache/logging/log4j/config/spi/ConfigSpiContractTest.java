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
package org.apache.logging.log4j.config.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Verifies that configuration SPI interfaces exist and expose the expected contract methods.
 */
class ConfigSpiContractTest {

    @Test
    void configurationSpiIsInterfaceWithExpectedMethods() throws NoSuchMethodException {
        assertThat(ConfigurationSPI.class).isInterface();
        assertThat(Modifier.isPublic(ConfigurationSPI.class.getModifiers())).isTrue();
        assertThat(ConfigurationSPI.CONTEXT_PROPERTIES).isEqualTo("ContextProperties");

        assertMethod(ConfigurationSPI.class, "getName", String.class);
        assertMethod(ConfigurationSPI.class, "getAppenders", java.util.Map.class);
        assertMethod(ConfigurationSPI.class, "getAppender", Object.class, String.class);
        assertMethod(ConfigurationSPI.class, "getLoggerConfig", Object.class, String.class);
        assertMethod(ConfigurationSPI.class, "getProperties", java.util.Map.class);
        assertMethod(ConfigurationSPI.class, "getRootLogger", Object.class);
    }

    @Test
    void configurationSourceSpiIsInterfaceWithExpectedMethods() throws NoSuchMethodException {
        assertThat(ConfigurationSourceSPI.class).isInterface();
        assertThat(Modifier.isPublic(ConfigurationSourceSPI.class.getModifiers()))
                .isTrue();

        assertMethod(ConfigurationSourceSPI.class, "getInputStream", java.io.InputStream.class);
        assertMethod(ConfigurationSourceSPI.class, "getLocation", String.class);
        assertMethod(ConfigurationSourceSPI.class, "getURI", java.net.URI.class);
    }

    @Test
    void propertyEnvironmentSpiIsInterfaceWithExpectedMethods() throws NoSuchMethodException {
        assertThat(PropertyEnvironmentSPI.class).isInterface();
        assertThat(Modifier.isPublic(PropertyEnvironmentSPI.class.getModifiers()))
                .isTrue();

        assertMethod(PropertyEnvironmentSPI.class, "getProperty", String.class, String.class);
        assertMethod(PropertyEnvironmentSPI.class, "getProperties", java.util.Map.class);
        assertMethod(PropertyEnvironmentSPI.class, "containsProperty", boolean.class, String.class);
    }

    @Test
    void propertyLookupSpiIsInterfaceWithExpectedMethods() throws NoSuchMethodException {
        assertThat(PropertyLookupSPI.class).isInterface();
        assertThat(Modifier.isPublic(PropertyLookupSPI.class.getModifiers())).isTrue();

        assertMethod(PropertyLookupSPI.class, "lookup", String.class, String.class);
        assertMethod(PropertyLookupSPI.class, "lookup", String.class, String.class, Object.class);
    }

    @Test
    void spiPackageContainsOnlyInterfacesAndConstants() {
        final Set<Class<?>> spiTypes = new HashSet<>(Arrays.asList(
                ConfigurationSPI.class,
                ConfigurationSourceSPI.class,
                PropertyEnvironmentSPI.class,
                PropertyLookupSPI.class));

        for (final Class<?> spiType : spiTypes) {
            assertThat(spiType.isInterface())
                    .as("%s should be an interface", spiType.getSimpleName())
                    .isTrue();
            assertThat(spiType.getEnclosingClass()).isNull();
            assertThat(spiType.getPackage().getName()).isEqualTo("org.apache.logging.log4j.config.spi");
        }

        assertThat(spiTypes.stream().map(Class::getSimpleName).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        "ConfigurationSPI", "ConfigurationSourceSPI", "PropertyEnvironmentSPI", "PropertyLookupSPI");
    }

    private static void assertMethod(
            final Class<?> type, final String name, final Class<?> returnType, final Class<?>... parameterTypes)
            throws NoSuchMethodException {
        final Method method = type.getMethod(name, parameterTypes);
        assertThat(method.getReturnType()).isEqualTo(returnType);
        assertThat(Modifier.isAbstract(method.getModifiers())).isTrue();
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }
}
