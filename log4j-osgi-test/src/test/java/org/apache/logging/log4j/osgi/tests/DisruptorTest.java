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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class DisruptorTest {

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return options(
                linkBundle("org.apache.logging.log4j.api"),
                linkBundle("org.apache.logging.log4j.core"),
                linkBundle("com.lmax.disruptor"),
                // required by Pax Exam's logging
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

    @Test
    public void testDisruptorLog() {
        // Logger context
        LoggerContext context = getLoggerContext();
        assertTrue("LoggerContext is an instance of AsyncLoggerContext", context instanceof AsyncLoggerContext);
        final CustomConfiguration custom = (CustomConfiguration) context.getConfiguration();
        // Logging
        final Logger logger = LogManager.getLogger(getClass());
        logger.info("Hello OSGI from Log4j2!");

        context.stop();
        assertEquals(1, custom.getEvents().size());
        final LogEvent event = custom.getEvents().get(0);
        assertEquals("Hello OSGI from Log4j2!", event.getMessage().getFormattedMessage());
        assertEquals(Level.INFO, event.getLevel());
        custom.clearEvents();
    }

    private static LoggerContext getLoggerContext() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        assertEquals("AsyncDefault", ctx.getName());
        return ctx;
    }
}
