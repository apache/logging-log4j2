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
package org.apache.logging.log4j.core.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.helpers.Charsets;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the JSONLayout class.
 */
public class JSONLayoutTest {
    static ConfigurationFactory cf = new BasicConfigurationFactory();

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    @BeforeClass
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
    }

    LoggerContext ctx = (LoggerContext) LogManager.getContext();

    Logger root = this.ctx.getLogger("");

    @Test
    public void testContentType() {
        final JSONLayout layout = JSONLayout.createLayout(null, null, null, null, null);
        assertEquals("application/json; charset=UTF-8", layout.getContentType());
    }

    @Test
    public void testDefaultCharset() {
        final JSONLayout layout = JSONLayout.createLayout(null, null, null, null, null);
        assertEquals(Charsets.UTF_8, layout.getCharset());
    }

    /**
     * Test case for MDC conversion pattern.
     */
    @Test
    public void testLayout() throws Exception {

        // set up appender
        final JSONLayout layout = JSONLayout.createLayout("true", "true", "true", "false", null);
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        this.root.addAppender(appender);
        this.root.setLevel(Level.DEBUG);

        // output starting message
        this.root.debug("starting mdc pattern test");

        this.root.debug("empty mdc");

        ThreadContext.put("key1", "value1");
        ThreadContext.put("key2", "value2");

        this.root.debug("filled mdc");

        ThreadContext.remove("key1");
        ThreadContext.remove("key2");

        this.root.error("finished mdc pattern test", new NullPointerException("test"));

        appender.stop();

        final List<String> list = appender.getMessages();

        // System.out.println(list);
        // [[, {, "logger":"root",, "timestamp":"1376676700199",, "level":"DEBUG",, "thread":"main",,
        // "message":"starting mdc pattern test",, "LocationInfo":{,
        // "class":"org.apache.logging.log4j.core.layout.JSONLayoutTest",, "method":"testLayout",,
        // "file":"JSONLayoutTest.java",, "line":"87", }, },, {, "logger":"root",, "timestamp":"1376676700203",,
        // "level":"DEBUG",, "thread":"main",, "message":"empty mdc",, "LocationInfo":{,
        // "class":"org.apache.logging.log4j.core.layout.JSONLayoutTest",, "method":"testLayout",,
        // "file":"JSONLayoutTest.java",, "line":"89", }, },, {, "logger":"root",, "timestamp":"1376676700204",,
        // "level":"DEBUG",, "thread":"main",, "message":"filled mdc",, "LocationInfo":{,
        // "class":"org.apache.logging.log4j.core.layout.JSONLayoutTest",, "method":"testLayout",,
        // "file":"JSONLayoutTest.java",, "line":"94", },, "Properties":[, {, "name":"key2",, "value":"value2", },, {,
        // "name":"key1",, "value":"value1", }, ], },, {, "logger":"root",, "timestamp":"1376676700204",,
        // "level":"ERROR",, "thread":"main",, "message":"finished mdc pattern test",,
        // "throwable":"java.lang.NullPointerException: test\\n\\tat org.apache.logging.log4j.core.layout.JSONLayoutTest.testLayout(JSONLayoutTest.java:99)\\n\\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\\n\\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)\\n\\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\\n\\tat java.lang.reflect.Method.invoke(Method.java:606)\\n\\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:47)\\n\\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\\n\\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:44)\\n\\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\\n\\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:271)\\n\\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:70)\\n\\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:50)\\n\\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:238)\\n\\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:63)\\n\\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:236)\\n\\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:53)\\n\\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:229)\\n\\tat org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)\\n\\tat org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)\\n\\tat org.junit.runners.ParentRunner.run(ParentRunner.java:309)\\n\\tat org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:50)\\n\\tat org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:38)\\n\\tat org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:467)\\n\\tat org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:683)\\n\\tat org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:390)\\n\\tat org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:197)\\n",,
        // "LocationInfo":{, "class":"org.apache.logging.log4j.core.layout.JSONLayoutTest",, "method":"testLayout",,
        // "file":"JSONLayoutTest.java",, "line":"99", }, },, ]]

        this.checkAt("[", 0, list);
        this.checkAt("{", 1, list);
        this.checkAt("\"logger\":\"root\",", 2, list);
        this.checkAt("\"level\":\"DEBUG\",", 4, list);
        this.checkAt("\"message\":\"starting mdc pattern test\",", 6, list);
    }

    private void checkAt(String expected, int lineIndex, List<String> list) {
        final String trimedLine = list.get(lineIndex).trim();
        assertTrue("Incorrect line index " + lineIndex + ": \"" + trimedLine + "\"", trimedLine.equals(expected));
    }
}
