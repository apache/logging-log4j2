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
package org.apache.logging.log4j.core.config.plugins.processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.PluginVisitorStrategy;
import org.apache.logging.log4j.core.config.plugins.validation.Constraint;
import org.apache.logging.log4j.core.config.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.core.config.plugins.visitors.PluginVisitor;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

/**
 * Fake constraint and plugin visitor that are accessed through reflection.
 */
public class FakeAnnotations {

    @Constraint(FakeConstraintValidator.class)
    public @interface FakeConstraint {}

    public static class FakeConstraintValidator implements ConstraintValidator<FakeConstraint> {
        @Override
        public void initialize(FakeConstraint annotation) {}

        @Override
        public boolean isValid(String name, Object value) {
            return false;
        }
    }

    @PluginVisitorStrategy(FakePluginVisitor.class)
    public @interface FakeAnnotation {}

    public static class FakePluginVisitor implements PluginVisitor<FakeAnnotation> {

        @Override
        public PluginVisitor<FakeAnnotation> setAnnotation(Annotation annotation) {
            return null;
        }

        @Override
        public PluginVisitor<FakeAnnotation> setAliases(String... aliases) {
            return null;
        }

        @Override
        public PluginVisitor<FakeAnnotation> setStrSubstitutor(StrSubstitutor substitutor) {
            return null;
        }

        @Override
        public PluginVisitor<FakeAnnotation> setMember(Member member) {
            return null;
        }

        @Override
        public Object visit(Configuration configuration, Node node, LogEvent event, StringBuilder log) {
            return null;
        }

        @Override
        public PluginVisitor<FakeAnnotation> setConversionType(Class<?> conversionType) {
            return null;
        }
    }
}
