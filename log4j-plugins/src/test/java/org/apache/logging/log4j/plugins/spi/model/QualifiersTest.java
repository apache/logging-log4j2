/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.apache.logging.log4j.plugins.spi.model;

import org.apache.logging.log4j.plugins.api.AliasFor;
import org.apache.logging.log4j.plugins.api.Named;
import org.apache.logging.log4j.plugins.api.QualifierType;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class QualifiersTest {
    @Test
    public void emptyQualifiersShouldContainDefaultQualifier() {
        final Qualifiers qualifiers = Qualifiers.fromQualifierAnnotations(Collections.emptyList());
        assertTrue(qualifiers.hasDefaultQualifier());
    }

    @Test
    public void qualifiersWithNamedOnlyShouldContainDefaultQualifier() {
        @Named
        class Foo {
        }
        final Qualifiers qualifiers = Qualifiers.fromQualifierAnnotations(Arrays.asList(Foo.class.getAnnotations()));
        assertTrue(qualifiers.hasDefaultQualifier());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @QualifierType
    public @interface Bar {
    }

    @Test
    public void qualifiersWithNonDefaultQualifiersShouldNotContainDefaultQualifier() {
        @Bar
        class Foo {
        }
        final Qualifiers qualifiers = Qualifiers.fromQualifierAnnotations(Arrays.asList(Foo.class.getAnnotations()));
        assertFalse(qualifiers.hasDefaultQualifier());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @AliasFor(Bar.class)
    public @interface BarAlias {
    }

    @Test
    public void qualifiersShouldAccountForAliases() {
        @Bar
        class Foo {
        }
        @BarAlias
        class FooAlias {
        }
        final Qualifiers foo = Qualifiers.fromQualifierAnnotations(Arrays.asList(Foo.class.getAnnotations()));
        final Qualifiers fooAlias = Qualifiers.fromQualifierAnnotations(Arrays.asList(FooAlias.class.getAnnotations()));
        assertEquals(foo, fooAlias);
    }
}