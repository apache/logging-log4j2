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
package org.apache.log4j;

import java.io.StringWriter;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Note that this test must clean up after itself or it may cause other tests to fail.
 */
public class VelocityTest {

private static LoggerContext context;
    
    @BeforeClass
    public static void setupClass() {
        context = LoggerContext.getContext(false);
    }

    @AfterClass
    public static void tearDownClass() {
        Configurator.shutdown(context);
        StatusLogger.getLogger().reset();
    }    
    
    @Test
    public void testVelocity() {
        Velocity.init();
        final VelocityContext vContext = new VelocityContext();
        vContext.put("name", new String("Velocity"));

        final Template template = Velocity.getTemplate("target/test-classes/hello.vm");

        final StringWriter sw = new StringWriter();

        template.merge(vContext, sw);
    }
}
