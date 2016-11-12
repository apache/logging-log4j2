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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.jackson.XmlConstants;

// Lines too long...
//CHECKSTYLE:OFF
/**
 * Appends a series of {@code event} elements as defined in the <a href="log4j.dtd">log4j.dtd</a>.
 *
 * <h3>Complete well-formed XML vs. fragment XML</h3>
 * <p>
 * If you configure {@code complete="true"}, the appender outputs a well-formed XML document where the default namespace is the log4j
 * namespace {@value XmlConstants#XML_NAMESPACE}. By default, with {@code complete="false"}, you should include the output as an
 * <em>external entity</em> in a separate file to form a well-formed XML document.
 * </p>
 * <p>
 * A well-formed XML document follows this pattern:
 * </p>
 * <pre>
&lt;Event xmlns=&quot;http://logging.apache.org/log4j/2.0/events&quot; timeMillis=&quot;1&quot; thread=&quot;MyThreadName&quot; level=&quot;DEBUG&quot; loggerName=&quot;a.B&quot; loggerFQCN=&quot;f.q.c.n&quot; endOfBatch=&quot;false&quot;&gt;
    &lt;Marker name=&quot;Marker1&quot;&gt;
        &lt;Parents&gt;
            &lt;Parents name=&quot;ParentMarker1&quot;&gt;
                &lt;Parents&gt;
                    &lt;Parents name=&quot;GrandMotherMarker&quot;/&gt;
                    &lt;Parents name=&quot;GrandFatherMarker&quot;/&gt;
                &lt;/Parents&gt;
            &lt;/Parents&gt;
            &lt;Parents name=&quot;GrandFatherMarker&quot;/&gt;
        &lt;/Parents&gt;
    &lt;/Marker&gt;
    &lt;Message&gt;Msg&lt;/Message&gt;
    &lt;ContextMap&gt;
        &lt;item key=&quot;MDC.B&quot; value=&quot;B_Value&quot;/&gt;
        &lt;item key=&quot;MDC.A&quot; value=&quot;A_Value&quot;/&gt;
    &lt;/ContextMap&gt;
    &lt;ContextStack&gt;
        &lt;ContextStack&gt;stack_msg1&lt;/ContextStack&gt;
        &lt;ContextStack&gt;stack_msg2&lt;/ContextStack&gt;
    &lt;/ContextStack&gt;
    &lt;Source class=&quot;org.apache.logging.log4j.core.layout.LogEventFixtures&quot; method=&quot;createLogEvent&quot; file=&quot;LogEventFixtures.java&quot; line=&quot;54&quot;/&gt;
    &lt;Thrown commonElementCount=&quot;0&quot; localizedMessage=&quot;testIOEx&quot; message=&quot;testIOEx&quot; name=&quot;java.io.IOException&quot;&gt;
        &lt;Cause commonElementCount=&quot;27&quot; localizedMessage=&quot;testNPEx&quot; message=&quot;testNPEx&quot; name=&quot;java.lang.NullPointerException&quot;&gt;
            &lt;ExtendedStackTrace&gt;
                &lt;ExtendedStackTrace class=&quot;org.apache.logging.log4j.core.layout.LogEventFixtures&quot; method=&quot;createLogEvent&quot; file=&quot;LogEventFixtures.java&quot; line=&quot;53&quot; exact=&quot;false&quot; location=&quot;test-classes/&quot; version=&quot;?&quot;/&gt;
            &lt;/ExtendedStackTrace&gt;
        &lt;/Cause&gt;
        &lt;ExtendedStackTrace&gt;
            &lt;ExtendedStackTrace class=&quot;org.apache.logging.log4j.core.layout.LogEventFixtures&quot; method=&quot;createLogEvent&quot; file=&quot;LogEventFixtures.java&quot; line=&quot;56&quot; exact=&quot;true&quot; location=&quot;test-classes/&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.apache.logging.log4j.core.layout.XmlLayoutTest&quot; method=&quot;testAllFeatures&quot; file=&quot;XmlLayoutTest.java&quot; line=&quot;122&quot; exact=&quot;true&quot; location=&quot;test-classes/&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.apache.logging.log4j.core.layout.XmlLayoutTest&quot; method=&quot;testLocationOnCompactOnMdcOn&quot; file=&quot;XmlLayoutTest.java&quot; line=&quot;270&quot; exact=&quot;true&quot; location=&quot;test-classes/&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;sun.reflect.NativeMethodAccessorImpl&quot; method=&quot;invoke&quot; line=&quot;-1&quot; exact=&quot;false&quot; location=&quot;?&quot; version=&quot;1.7.0_55&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;sun.reflect.NativeMethodAccessorImpl&quot; method=&quot;invoke&quot; line=&quot;-1&quot; exact=&quot;false&quot; location=&quot;?&quot; version=&quot;1.7.0_55&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;sun.reflect.DelegatingMethodAccessorImpl&quot; method=&quot;invoke&quot; line=&quot;-1&quot; exact=&quot;false&quot; location=&quot;?&quot; version=&quot;1.7.0_55&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;java.lang.reflect.Method&quot; method=&quot;invoke&quot; line=&quot;-1&quot; exact=&quot;false&quot; location=&quot;?&quot; version=&quot;1.7.0_55&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.runners.model.FrameworkMethod$1&quot; method=&quot;runReflectiveCall&quot; file=&quot;FrameworkMethod.java&quot; line=&quot;47&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.internal.runners.model.ReflectiveCallable&quot; method=&quot;run&quot; file=&quot;ReflectiveCallable.java&quot; line=&quot;12&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.runners.model.FrameworkMethod&quot; method=&quot;invokeExplosively&quot; file=&quot;FrameworkMethod.java&quot; line=&quot;44&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.internal.runners.statements.InvokeMethod&quot; method=&quot;evaluate&quot; file=&quot;InvokeMethod.java&quot; line=&quot;17&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner&quot; method=&quot;runLeaf&quot; file=&quot;ParentRunner.java&quot; line=&quot;271&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.runners.BlockJUnit4ClassRunner&quot; method=&quot;runChild&quot; file=&quot;BlockJUnit4ClassRunner.java&quot; line=&quot;70&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.runners.BlockJUnit4ClassRunner&quot; method=&quot;runChild&quot; file=&quot;BlockJUnit4ClassRunner.java&quot; line=&quot;50&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner$3&quot; method=&quot;run&quot; file=&quot;ParentRunner.java&quot; line=&quot;238&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner$1&quot; method=&quot;schedule&quot; file=&quot;ParentRunner.java&quot; line=&quot;63&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner&quot; method=&quot;runChildren&quot; file=&quot;ParentRunner.java&quot; line=&quot;236&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner&quot; method=&quot;access$000&quot; file=&quot;ParentRunner.java&quot; line=&quot;53&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner$2&quot; method=&quot;evaluate&quot; file=&quot;ParentRunner.java&quot; line=&quot;229&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.internal.runners.statements.RunBefores&quot; method=&quot;evaluate&quot; file=&quot;RunBefores.java&quot; line=&quot;26&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.internal.runners.statements.RunAfters&quot; method=&quot;evaluate&quot; file=&quot;RunAfters.java&quot; line=&quot;27&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner&quot; method=&quot;run&quot; file=&quot;ParentRunner.java&quot; line=&quot;309&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference&quot; method=&quot;run&quot; file=&quot;JUnit4TestReference.java&quot; line=&quot;50&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.TestExecution&quot; method=&quot;run&quot; file=&quot;TestExecution.java&quot; line=&quot;38&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.RemoteTestRunner&quot; method=&quot;runTests&quot; file=&quot;RemoteTestRunner.java&quot; line=&quot;467&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.RemoteTestRunner&quot; method=&quot;runTests&quot; file=&quot;RemoteTestRunner.java&quot; line=&quot;683&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.RemoteTestRunner&quot; method=&quot;run&quot; file=&quot;RemoteTestRunner.java&quot; line=&quot;390&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
            &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.RemoteTestRunner&quot; method=&quot;main&quot; file=&quot;RemoteTestRunner.java&quot; line=&quot;197&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
        &lt;/ExtendedStackTrace&gt;
        &lt;Suppressed&gt;
            &lt;Suppressed commonElementCount=&quot;0&quot; localizedMessage=&quot;I am suppressed exception 1&quot; message=&quot;I am suppressed exception 1&quot; name=&quot;java.lang.IndexOutOfBoundsException&quot;&gt;
                &lt;ExtendedStackTrace&gt;
                    &lt;ExtendedStackTrace class=&quot;org.apache.logging.log4j.core.layout.LogEventFixtures&quot; method=&quot;createLogEvent&quot; file=&quot;LogEventFixtures.java&quot; line=&quot;57&quot; exact=&quot;true&quot; location=&quot;test-classes/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.apache.logging.log4j.core.layout.XmlLayoutTest&quot; method=&quot;testAllFeatures&quot; file=&quot;XmlLayoutTest.java&quot; line=&quot;122&quot; exact=&quot;true&quot; location=&quot;test-classes/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.apache.logging.log4j.core.layout.XmlLayoutTest&quot; method=&quot;testLocationOnCompactOnMdcOn&quot; file=&quot;XmlLayoutTest.java&quot; line=&quot;270&quot; exact=&quot;true&quot; location=&quot;test-classes/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;sun.reflect.NativeMethodAccessorImpl&quot; method=&quot;invoke&quot; line=&quot;-1&quot; exact=&quot;false&quot; location=&quot;?&quot; version=&quot;1.7.0_55&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;sun.reflect.NativeMethodAccessorImpl&quot; method=&quot;invoke&quot; line=&quot;-1&quot; exact=&quot;false&quot; location=&quot;?&quot; version=&quot;1.7.0_55&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;sun.reflect.DelegatingMethodAccessorImpl&quot; method=&quot;invoke&quot; line=&quot;-1&quot; exact=&quot;false&quot; location=&quot;?&quot; version=&quot;1.7.0_55&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;java.lang.reflect.Method&quot; method=&quot;invoke&quot; line=&quot;-1&quot; exact=&quot;false&quot; location=&quot;?&quot; version=&quot;1.7.0_55&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.model.FrameworkMethod$1&quot; method=&quot;runReflectiveCall&quot; file=&quot;FrameworkMethod.java&quot; line=&quot;47&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.internal.runners.model.ReflectiveCallable&quot; method=&quot;run&quot; file=&quot;ReflectiveCallable.java&quot; line=&quot;12&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.model.FrameworkMethod&quot; method=&quot;invokeExplosively&quot; file=&quot;FrameworkMethod.java&quot; line=&quot;44&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.internal.runners.statements.InvokeMethod&quot; method=&quot;evaluate&quot; file=&quot;InvokeMethod.java&quot; line=&quot;17&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner&quot; method=&quot;runLeaf&quot; file=&quot;ParentRunner.java&quot; line=&quot;271&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.BlockJUnit4ClassRunner&quot; method=&quot;runChild&quot; file=&quot;BlockJUnit4ClassRunner.java&quot; line=&quot;70&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.BlockJUnit4ClassRunner&quot; method=&quot;runChild&quot; file=&quot;BlockJUnit4ClassRunner.java&quot; line=&quot;50&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner$3&quot; method=&quot;run&quot; file=&quot;ParentRunner.java&quot; line=&quot;238&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner$1&quot; method=&quot;schedule&quot; file=&quot;ParentRunner.java&quot; line=&quot;63&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner&quot; method=&quot;runChildren&quot; file=&quot;ParentRunner.java&quot; line=&quot;236&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner&quot; method=&quot;access$000&quot; file=&quot;ParentRunner.java&quot; line=&quot;53&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner$2&quot; method=&quot;evaluate&quot; file=&quot;ParentRunner.java&quot; line=&quot;229&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.internal.runners.statements.RunBefores&quot; method=&quot;evaluate&quot; file=&quot;RunBefores.java&quot; line=&quot;26&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.internal.runners.statements.RunAfters&quot; method=&quot;evaluate&quot; file=&quot;RunAfters.java&quot; line=&quot;27&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner&quot; method=&quot;run&quot; file=&quot;ParentRunner.java&quot; line=&quot;309&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference&quot; method=&quot;run&quot; file=&quot;JUnit4TestReference.java&quot; line=&quot;50&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.TestExecution&quot; method=&quot;run&quot; file=&quot;TestExecution.java&quot; line=&quot;38&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.RemoteTestRunner&quot; method=&quot;runTests&quot; file=&quot;RemoteTestRunner.java&quot; line=&quot;467&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.RemoteTestRunner&quot; method=&quot;runTests&quot; file=&quot;RemoteTestRunner.java&quot; line=&quot;683&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.RemoteTestRunner&quot; method=&quot;run&quot; file=&quot;RemoteTestRunner.java&quot; line=&quot;390&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.RemoteTestRunner&quot; method=&quot;main&quot; file=&quot;RemoteTestRunner.java&quot; line=&quot;197&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
                &lt;/ExtendedStackTrace&gt;
            &lt;/Suppressed&gt;
            &lt;Suppressed commonElementCount=&quot;0&quot; localizedMessage=&quot;I am suppressed exception 2&quot; message=&quot;I am suppressed exception 2&quot; name=&quot;java.lang.IndexOutOfBoundsException&quot;&gt;
                &lt;ExtendedStackTrace&gt;
                    &lt;ExtendedStackTrace class=&quot;org.apache.logging.log4j.core.layout.LogEventFixtures&quot; method=&quot;createLogEvent&quot; file=&quot;LogEventFixtures.java&quot; line=&quot;58&quot; exact=&quot;true&quot; location=&quot;test-classes/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.apache.logging.log4j.core.layout.XmlLayoutTest&quot; method=&quot;testAllFeatures&quot; file=&quot;XmlLayoutTest.java&quot; line=&quot;122&quot; exact=&quot;true&quot; location=&quot;test-classes/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.apache.logging.log4j.core.layout.XmlLayoutTest&quot; method=&quot;testLocationOnCompactOnMdcOn&quot; file=&quot;XmlLayoutTest.java&quot; line=&quot;270&quot; exact=&quot;true&quot; location=&quot;test-classes/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;sun.reflect.NativeMethodAccessorImpl&quot; method=&quot;invoke&quot; line=&quot;-1&quot; exact=&quot;false&quot; location=&quot;?&quot; version=&quot;1.7.0_55&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;sun.reflect.NativeMethodAccessorImpl&quot; method=&quot;invoke&quot; line=&quot;-1&quot; exact=&quot;false&quot; location=&quot;?&quot; version=&quot;1.7.0_55&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;sun.reflect.DelegatingMethodAccessorImpl&quot; method=&quot;invoke&quot; line=&quot;-1&quot; exact=&quot;false&quot; location=&quot;?&quot; version=&quot;1.7.0_55&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;java.lang.reflect.Method&quot; method=&quot;invoke&quot; line=&quot;-1&quot; exact=&quot;false&quot; location=&quot;?&quot; version=&quot;1.7.0_55&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.model.FrameworkMethod$1&quot; method=&quot;runReflectiveCall&quot; file=&quot;FrameworkMethod.java&quot; line=&quot;47&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.internal.runners.model.ReflectiveCallable&quot; method=&quot;run&quot; file=&quot;ReflectiveCallable.java&quot; line=&quot;12&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.model.FrameworkMethod&quot; method=&quot;invokeExplosively&quot; file=&quot;FrameworkMethod.java&quot; line=&quot;44&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.internal.runners.statements.InvokeMethod&quot; method=&quot;evaluate&quot; file=&quot;InvokeMethod.java&quot; line=&quot;17&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner&quot; method=&quot;runLeaf&quot; file=&quot;ParentRunner.java&quot; line=&quot;271&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.BlockJUnit4ClassRunner&quot; method=&quot;runChild&quot; file=&quot;BlockJUnit4ClassRunner.java&quot; line=&quot;70&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.BlockJUnit4ClassRunner&quot; method=&quot;runChild&quot; file=&quot;BlockJUnit4ClassRunner.java&quot; line=&quot;50&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner$3&quot; method=&quot;run&quot; file=&quot;ParentRunner.java&quot; line=&quot;238&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner$1&quot; method=&quot;schedule&quot; file=&quot;ParentRunner.java&quot; line=&quot;63&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner&quot; method=&quot;runChildren&quot; file=&quot;ParentRunner.java&quot; line=&quot;236&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner&quot; method=&quot;access$000&quot; file=&quot;ParentRunner.java&quot; line=&quot;53&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner$2&quot; method=&quot;evaluate&quot; file=&quot;ParentRunner.java&quot; line=&quot;229&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.internal.runners.statements.RunBefores&quot; method=&quot;evaluate&quot; file=&quot;RunBefores.java&quot; line=&quot;26&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.internal.runners.statements.RunAfters&quot; method=&quot;evaluate&quot; file=&quot;RunAfters.java&quot; line=&quot;27&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.junit.runners.ParentRunner&quot; method=&quot;run&quot; file=&quot;ParentRunner.java&quot; line=&quot;309&quot; exact=&quot;true&quot; location=&quot;junit-4.11.jar&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference&quot; method=&quot;run&quot; file=&quot;JUnit4TestReference.java&quot; line=&quot;50&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.TestExecution&quot; method=&quot;run&quot; file=&quot;TestExecution.java&quot; line=&quot;38&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.RemoteTestRunner&quot; method=&quot;runTests&quot; file=&quot;RemoteTestRunner.java&quot; line=&quot;467&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.RemoteTestRunner&quot; method=&quot;runTests&quot; file=&quot;RemoteTestRunner.java&quot; line=&quot;683&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.RemoteTestRunner&quot; method=&quot;run&quot; file=&quot;RemoteTestRunner.java&quot; line=&quot;390&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
                    &lt;ExtendedStackTrace class=&quot;org.eclipse.jdt.internal.junit.runner.RemoteTestRunner&quot; method=&quot;main&quot; file=&quot;RemoteTestRunner.java&quot; line=&quot;197&quot; exact=&quot;true&quot; location=&quot;.cp/&quot; version=&quot;?&quot;/&gt;
                &lt;/ExtendedStackTrace&gt;
            &lt;/Suppressed&gt;
        &lt;/Suppressed&gt;
    &lt;/Thrown&gt;
&lt;/Event&gt;
</pre>
 * <p>
 * If {@code complete="false"}, the appender does not write the XML processing instruction and the root element.
 * </p>
 * <p>
 * This approach enforces the independence of the XmlLayout and the appender where you embed it.
 * </p>
 * <h3>Encoding</h3>
 * <p>
 * Appenders using this layout should have their {@code charset} set to {@code UTF-8} or {@code UTF-16}, otherwise events containing non
 * ASCII characters could result in corrupted log files.
 * </p>
 * <h3>Pretty vs. compact XML</h3>
 * <p>
 * By default, the XML layout is not compact (compact = not "pretty") with {@code compact="false"}, which means the appender uses
 * end-of-line characters and indents lines to format the XML. If {@code compact="true"}, then no end-of-line or indentation is used.
 * Message content may contain, of course, end-of-lines.
 * </p>
 */
//CHECKSTYLE:ON
@Plugin(name = "XmlLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class XmlLayout extends AbstractJacksonLayout {

    private static final String ROOT_TAG = "Events";

    protected XmlLayout(final boolean locationInfo, final boolean properties, final boolean complete,
                        final boolean compact, final Charset charset, final boolean includeStacktrace) {
        super(null, new JacksonFactory.XML(includeStacktrace).newWriter(
                locationInfo, properties, compact), charset, compact, complete, false, null, null);
    }

    /**
     * Returns appropriate XML headers.
     * <ol>
     * <li>XML processing instruction</li>
     * <li>XML root element</li>
     * </ol>
     *
     * @return a byte array containing the header.
     */
    @Override
    public byte[] getHeader() {
        if (!complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("<?xml version=\"1.0\" encoding=\"");
        buf.append(this.getCharset().name());
        buf.append("\"?>");
        buf.append(this.eol);
        // Make the log4j namespace the default namespace, no need to use more space with a namespace prefix.
        buf.append('<');
        buf.append(ROOT_TAG);
        buf.append(" xmlns=\"" + XmlConstants.XML_NAMESPACE + "\">");
        buf.append(this.eol);
        return buf.toString().getBytes(this.getCharset());
    }

    /**
     * Returns appropriate XML footer.
     *
     * @return a byte array containing the footer, closing the XML root element.
     */
    @Override
    public byte[] getFooter() {
        if (!complete) {
            return null;
        }
        return getBytes("</" + ROOT_TAG + '>' + this.eol);
    }

    /**
     * Gets this XmlLayout's content format. Specified by:
     * <ul>
     * <li>Key: "dtd" Value: "log4j-events.dtd"</li>
     * <li>Key: "version" Value: "2.0"</li>
     * </ul>
     * 
     * @return Map of content format keys supporting XmlLayout
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>();
        // result.put("dtd", "log4j-events.dtd");
        result.put("xsd", "log4j-events.xsd");
        result.put("version", "2.0");
        return result;
    }

    /**
     * @return The content type.
     */
    @Override
    public String getContentType() {
        return "text/xml; charset=" + this.getCharset();
    }

    /**
     * Creates an XML Layout.
     *
     * @param locationInfo If "true", includes the location information in the generated XML.
     * @param properties If "true", includes the thread context map in the generated XML.
     * @param complete If "true", includes the XML header and footer, defaults to "false".
     * @param compact If "true", does not use end-of-lines and indentation, defaults to "false".
     * @param charset The character set to use, if {@code null}, uses "UTF-8".
     * @param includeStacktrace
     *            If "true", includes the stacktrace of any Throwable in the generated XML, defaults to "true".
     * @return An XML Layout.
     */
    @PluginFactory
    public static XmlLayout createLayout(
            // @formatter:off
            @PluginAttribute(value = "locationInfo") final boolean locationInfo,
            @PluginAttribute(value = "properties") final boolean properties,
            @PluginAttribute(value = "complete") final boolean complete,
            @PluginAttribute(value = "compact") final boolean compact,
            @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charset,
            @PluginAttribute(value = "includeStacktrace", defaultBoolean = true) final boolean includeStacktrace)
            // @formatter:on
    {
        return new XmlLayout(locationInfo, properties, complete, compact, charset, includeStacktrace);
    }

    /**
     * Creates an XML Layout using the default settings.
     *
     * @return an XML Layout.
     */
    public static XmlLayout createDefaultLayout() {
        return new XmlLayout(false, false, false, false, StandardCharsets.UTF_8, true);
    }
}
