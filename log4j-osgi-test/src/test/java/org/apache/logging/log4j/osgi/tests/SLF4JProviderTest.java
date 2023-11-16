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
package org.apache.logging.log4j.osgi.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.slf4j.SLF4JLoggerContextFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SLF4JProviderTest {

    @Inject
    private BundleContext context;

    @Configuration
    public Option[] config() {
        return options(
                linkBundle("org.apache.logging.log4j.api"),
                linkBundle("org.apache.logging.log4j.to.slf4j"),
                linkBundle("org.objectweb.asm"),
                linkBundle("org.objectweb.asm.commons"),
                linkBundle("org.objectweb.asm.tree"),
                linkBundle("org.objectweb.asm.tree.analysis"),
                linkBundle("org.objectweb.asm.util"),
                linkBundle("org.apache.aries.spifly.dynamic.bundle").startLevel(2),
                linkBundle("slf4j.api"),
                linkBundle("ch.qos.logback.classic"),
                linkBundle("ch.qos.logback.core"),
                junitBundles());
    }

    @Test(timeout = 10_000L)
    public void testSlf4jFactoryResolves() {
        final Optional<Bundle> slf4jBundle = Stream.of(context.getBundles())
                .filter(b -> "org.apache.logging.log4j.to.slf4j".equals(b.getSymbolicName()))
                .findAny();
        assertTrue(slf4jBundle.isPresent());
        final LoggerContextFactory factory = LogManager.getFactory();
        assertEquals(SLF4JLoggerContextFactory.class, factory.getClass());
    }
}
