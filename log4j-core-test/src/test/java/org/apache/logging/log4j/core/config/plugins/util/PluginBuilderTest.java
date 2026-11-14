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
package org.apache.logging.log4j.core.config.plugins.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginLoggerContext;
import org.apache.logging.log4j.core.config.plugins.PluginNode;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.core.config.plugins.PluginVisitorStrategy;
import org.apache.logging.log4j.core.config.plugins.processor.PluginEntry;
import org.apache.logging.log4j.core.config.plugins.validation.Constraint;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.config.plugins.validation.validators.RequiredValidator;
import org.apache.logging.log4j.core.config.plugins.visitors.PluginValueVisitor;
import org.apache.logging.log4j.core.util.Builder;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PluginBuilderTest {

    private static final String CORE_REACHABILITY_METADATA =
            "META-INF/native-image/log4j-generated/org.apache.logging.log4j/log4j-core/reflect-config.json";

    /**
     * The fields that the GraalVM metadata of {@code log4j-core} registers, as {@code <class name>#<field name>}.
     */
    private static Set<String> coreRegisteredFields;

    @BeforeAll
    static void readCoreReachabilityMetadata() throws IOException {
        final URL metadata = PluginBuilder.class.getClassLoader().getResource(CORE_REACHABILITY_METADATA);
        assertThat(metadata)
                .as("`%s` is missing: build `log4j-core` with Maven", CORE_REACHABILITY_METADATA)
                .isNotNull();
        coreRegisteredFields = new HashSet<>();
        try (final InputStream input = metadata.openStream()) {
            for (final JsonNode type : new ObjectMapper().readTree(input)) {
                for (final JsonNode field : type.path("fields")) {
                    coreRegisteredFields.add(
                            type.get("name").asText() + "#" + field.get("name").asText());
                }
            }
        }
    }

    @Test
    void getPluginFieldsReturnsOnlyFieldsWithVisitorOrConstraintAnnotations() {
        assertThat(PluginBuilder.getPluginFields(FakeBuilder.class))
                .extracting(Field::getName)
                .containsExactlyInAnyOrder(
                        "builderAttribute",
                        "attribute",
                        "element",
                        "configuration",
                        "node",
                        "value",
                        "loggerContext",
                        "customVisitor",
                        "requiredOnly",
                        "customConstraint",
                        "inheritedBuilderAttribute");
    }

    static Stream<Class<?>> coreBuilderClasses() throws ReflectiveOperationException {
        final URL coreLocation =
                PluginBuilder.class.getProtectionDomain().getCodeSource().getLocation();
        final Set<Class<?>> builderClasses = new TreeSet<>(Comparator.comparing(Class::getName));
        for (final List<PluginType<?>> pluginTypes :
                PluginRegistry.getInstance().loadFromMainClassLoader().values()) {
            for (final PluginType<?> pluginType : pluginTypes) {
                final Class<?> pluginClass = pluginType.getPluginClass();
                if (coreLocation.equals(
                        pluginClass.getProtectionDomain().getCodeSource().getLocation())) {
                    final Builder<?> builder = PluginBuilder.createBuilder(pluginClass);
                    if (builder != null) {
                        builderClasses.add(builder.getClass());
                    }
                }
            }
        }
        return builderClasses.stream();
    }

    /**
     * Checks that the GraalVM metadata of {@code log4j-core} registers all the fields that {@link PluginBuilder} uses.
     */
    @ParameterizedTest
    @MethodSource("coreBuilderClasses")
    void pluginFieldsOfCoreBuildersAreRegisteredForGraalVm(final Class<?> builderClass) {
        assertThat(PluginBuilder.getPluginFields(builderClass))
                .extracting(field -> field.getDeclaringClass().getName() + "#" + field.getName())
                .filteredOn(name -> !coreRegisteredFields.contains(name))
                .as("Plugin fields without GraalVM reflection metadata")
                .isEmpty();
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_16)
    void buildUsesReflectionOnlyOnPluginFields() {
        final PluginType<JdkSubclassPlugin> type =
                new PluginType<>(new PluginEntry(), JdkSubclassPlugin.class, "Dummy");
        final Node node = new Node(null, "Dummy", type);
        node.getAttributes().put("attribute", "value");
        final Object plugin = new PluginBuilder(type)
                .withConfiguration(new NullConfiguration())
                .withConfigurationNode(node)
                .build();
        assertThat(plugin).isEqualTo("value");
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_16)
    void validateFieldsUsesReflectionOnlyOnPluginFields() {
        final JdkSubclassBuilder builder = new JdkSubclassBuilder();
        assertThat(PluginBuilder.validateFields(builder, "Dummy")).isTrue();
        builder.requiredOnly = null;
        assertThat(PluginBuilder.validateFields(builder, "Dummy")).isFalse();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @PluginVisitorStrategy(PluginValueVisitor.class)
    private @interface CustomVisitor {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @Constraint(RequiredValidator.class)
    private @interface CustomConstraint {}

    @SuppressWarnings("unused")
    private abstract static class FakeParentBuilder {

        private static final String INHERITED_CONSTANT = "inherited";

        @PluginBuilderAttribute
        @SuppressWarnings("log4j.public.setter")
        private String inheritedBuilderAttribute;

        private String inheritedPlain;
    }

    @SuppressWarnings("unused")
    private static final class FakeBuilder extends FakeParentBuilder implements Builder<Object> {

        private static final Logger LOGGER = StatusLogger.getLogger();

        private static final String CONSTANT = "constant";

        @PluginBuilderAttribute
        @SuppressWarnings("log4j.public.setter")
        private String builderAttribute;

        @PluginAttribute("attribute")
        private String attribute;

        @PluginElement("Layout")
        private Layout<?> element;

        @PluginConfiguration
        private Configuration configuration;

        @PluginNode
        private Node node;

        @PluginValue("value")
        private String value;

        @PluginLoggerContext
        private LoggerContext loggerContext;

        @CustomVisitor
        private String customVisitor;

        @Required
        private String requiredOnly;

        @CustomConstraint
        private String customConstraint;

        @PluginAliases("alias")
        private String aliasesOnly;

        @Deprecated
        private String otherAnnotation;

        private String plain;

        @Override
        public Object build() {
            return null;
        }
    }

    private static final class JdkSubclassPlugin {

        @PluginBuilderFactory
        static JdkSubclassBuilder newBuilder() {
            return new JdkSubclassBuilder();
        }
    }

    /**
     * Has fields that do not allow reflection, like fields that have no metadata in a GraalVM native image.
     * <p>
     *     On Java 16 and later, reflection on the private fields of the {@link AtomicInteger} superclass fails,
     *     because their JDK package is not open.
     * </p>
     */
    @SuppressWarnings("serial")
    private static final class JdkSubclassBuilder extends AtomicInteger implements Builder<Object> {

        @PluginAttribute("attribute")
        private String attribute;

        @Required
        private String requiredOnly = "default";

        @Override
        public Object build() {
            return attribute;
        }
    }
}
