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

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.util.Strings;

/**
 * Appends a series of YAML events as strings serialized as bytes.
 *
 * <p>
 * A YAML log event follows this pattern:
 * </p>
 *
 * <pre>---
timeMillis: 1
thread: "MyThreadName"
level: "DEBUG"
loggerName: "a.B"
marker:
  name: "Marker1"
  parents:
  - name: "ParentMarker1"
    parents:
    - name: "GrandMotherMarker"
    - name: "GrandFatherMarker"
  - name: "ParentMarker2"
message: "Msg"
thrown:
  commonElementCount: 0
  localizedMessage: "testIOEx"
  message: "testIOEx"
  name: "java.io.IOException"
  cause:
    commonElementCount: 27
    localizedMessage: "testNPEx"
    message: "testNPEx"
    name: "java.lang.NullPointerException"
    extendedStackTrace:
    - class: "org.apache.logging.log4j.core.layout.LogEventFixtures"
      method: "createLogEvent"
      file: "LogEventFixtures.java"
      line: 52
      exact: false
      location: "test-classes/"
      version: "?"
  extendedStackTrace:
  - class: "org.apache.logging.log4j.core.layout.LogEventFixtures"
    method: "createLogEvent"
    file: "LogEventFixtures.java"
    line: 55
    exact: true
    location: "test-classes/"
    version: "?"
  - class: "org.apache.logging.log4j.core.layout.YamlLayoutTest"
    method: "testAllFeatures"
    file: "YamlLayoutTest.java"
    line: 109
    exact: true
    location: "test-classes/"
    version: "?"
  - class: "org.apache.logging.log4j.core.layout.YamlLayoutTest"
    method: "testLocationOnCompactOffEventEolOffMdcOn"
    file: "YamlLayoutTest.java"
    line: 280
    exact: true
    location: "test-classes/"
    version: "?"
  - class: "sun.reflect.NativeMethodAccessorImpl"
    method: "invoke0"
    file: "NativeMethodAccessorImpl.java"
    line: -2
    exact: false
    location: "?"
    version: "1.7.0_79"
  - class: "sun.reflect.NativeMethodAccessorImpl"
    method: "invoke"
    file: "NativeMethodAccessorImpl.java"
    line: 57
    exact: false
    location: "?"
    version: "1.7.0_79"
  - class: "sun.reflect.DelegatingMethodAccessorImpl"
    method: "invoke"
    file: "DelegatingMethodAccessorImpl.java"
    line: 43
    exact: false
    location: "?"
    version: "1.7.0_79"
  - class: "java.lang.reflect.Method"
    method: "invoke"
    file: "Method.java"
    line: 606
    exact: false
    location: "?"
    version: "1.7.0_79"
  - class: "org.junit.runners.model.FrameworkMethod$1"
    method: "runReflectiveCall"
    file: "FrameworkMethod.java"
    line: 50
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.internal.runners.model.ReflectiveCallable"
    method: "run"
    file: "ReflectiveCallable.java"
    line: 12
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.runners.model.FrameworkMethod"
    method: "invokeExplosively"
    file: "FrameworkMethod.java"
    line: 47
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.internal.runners.statements.InvokeMethod"
    method: "evaluate"
    file: "InvokeMethod.java"
    line: 17
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.runners.ParentRunner"
    method: "runLeaf"
    file: "ParentRunner.java"
    line: 325
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.runners.BlockJUnit4ClassRunner"
    method: "runChild"
    file: "BlockJUnit4ClassRunner.java"
    line: 78
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.runners.BlockJUnit4ClassRunner"
    method: "runChild"
    file: "BlockJUnit4ClassRunner.java"
    line: 57
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.runners.ParentRunner$3"
    method: "run"
    file: "ParentRunner.java"
    line: 290
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.runners.ParentRunner$1"
    method: "schedule"
    file: "ParentRunner.java"
    line: 71
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.runners.ParentRunner"
    method: "runChildren"
    file: "ParentRunner.java"
    line: 288
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.runners.ParentRunner"
    method: "access$000"
    file: "ParentRunner.java"
    line: 58
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.runners.ParentRunner$2"
    method: "evaluate"
    file: "ParentRunner.java"
    line: 268
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.internal.runners.statements.RunBefores"
    method: "evaluate"
    file: "RunBefores.java"
    line: 26
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.internal.runners.statements.RunAfters"
    method: "evaluate"
    file: "RunAfters.java"
    line: 27
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.junit.runners.ParentRunner"
    method: "run"
    file: "ParentRunner.java"
    line: 363
    exact: true
    location: "junit-4.12.jar"
    version: "4.12"
  - class: "org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference"
    method: "run"
    file: "JUnit4TestReference.java"
    line: 86
    exact: true
    location: ".cp/"
    version: "?"
  - class: "org.eclipse.jdt.internal.junit.runner.TestExecution"
    method: "run"
    file: "TestExecution.java"
    line: 38
    exact: true
    location: ".cp/"
    version: "?"
  - class: "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"
    method: "runTests"
    file: "RemoteTestRunner.java"
    line: 459
    exact: true
    location: ".cp/"
    version: "?"
  - class: "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"
    method: "runTests"
    file: "RemoteTestRunner.java"
    line: 675
    exact: true
    location: ".cp/"
    version: "?"
  - class: "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"
    method: "run"
    file: "RemoteTestRunner.java"
    line: 382
    exact: true
    location: ".cp/"
    version: "?"
  - class: "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"
    method: "main"
    file: "RemoteTestRunner.java"
    line: 192
    exact: true
    location: ".cp/"
    version: "?"
  suppressed:
  - commonElementCount: 0
    localizedMessage: "I am suppressed exception 1"
    message: "I am suppressed exception 1"
    name: "java.lang.IndexOutOfBoundsException"
    extendedStackTrace:
    - class: "org.apache.logging.log4j.core.layout.LogEventFixtures"
      method: "createLogEvent"
      file: "LogEventFixtures.java"
      line: 56
      exact: true
      location: "test-classes/"
      version: "?"
    - class: "org.apache.logging.log4j.core.layout.YamlLayoutTest"
      method: "testAllFeatures"
      file: "YamlLayoutTest.java"
      line: 109
      exact: true
      location: "test-classes/"
      version: "?"
    - class: "org.apache.logging.log4j.core.layout.YamlLayoutTest"
      method: "testLocationOnCompactOffEventEolOffMdcOn"
      file: "YamlLayoutTest.java"
      line: 280
      exact: true
      location: "test-classes/"
      version: "?"
    - class: "sun.reflect.NativeMethodAccessorImpl"
      method: "invoke0"
      file: "NativeMethodAccessorImpl.java"
      line: -2
      exact: false
      location: "?"
      version: "1.7.0_79"
    - class: "sun.reflect.NativeMethodAccessorImpl"
      method: "invoke"
      file: "NativeMethodAccessorImpl.java"
      line: 57
      exact: false
      location: "?"
      version: "1.7.0_79"
    - class: "sun.reflect.DelegatingMethodAccessorImpl"
      method: "invoke"
      file: "DelegatingMethodAccessorImpl.java"
      line: 43
      exact: false
      location: "?"
      version: "1.7.0_79"
    - class: "java.lang.reflect.Method"
      method: "invoke"
      file: "Method.java"
      line: 606
      exact: false
      location: "?"
      version: "1.7.0_79"
    - class: "org.junit.runners.model.FrameworkMethod$1"
      method: "runReflectiveCall"
      file: "FrameworkMethod.java"
      line: 50
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.internal.runners.model.ReflectiveCallable"
      method: "run"
      file: "ReflectiveCallable.java"
      line: 12
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.model.FrameworkMethod"
      method: "invokeExplosively"
      file: "FrameworkMethod.java"
      line: 47
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.internal.runners.statements.InvokeMethod"
      method: "evaluate"
      file: "InvokeMethod.java"
      line: 17
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner"
      method: "runLeaf"
      file: "ParentRunner.java"
      line: 325
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.BlockJUnit4ClassRunner"
      method: "runChild"
      file: "BlockJUnit4ClassRunner.java"
      line: 78
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.BlockJUnit4ClassRunner"
      method: "runChild"
      file: "BlockJUnit4ClassRunner.java"
      line: 57
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner$3"
      method: "run"
      file: "ParentRunner.java"
      line: 290
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner$1"
      method: "schedule"
      file: "ParentRunner.java"
      line: 71
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner"
      method: "runChildren"
      file: "ParentRunner.java"
      line: 288
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner"
      method: "access$000"
      file: "ParentRunner.java"
      line: 58
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner$2"
      method: "evaluate"
      file: "ParentRunner.java"
      line: 268
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.internal.runners.statements.RunBefores"
      method: "evaluate"
      file: "RunBefores.java"
      line: 26
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.internal.runners.statements.RunAfters"
      method: "evaluate"
      file: "RunAfters.java"
      line: 27
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner"
      method: "run"
      file: "ParentRunner.java"
      line: 363
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference"
      method: "run"
      file: "JUnit4TestReference.java"
      line: 86
      exact: true
      location: ".cp/"
      version: "?"
    - class: "org.eclipse.jdt.internal.junit.runner.TestExecution"
      method: "run"
      file: "TestExecution.java"
      line: 38
      exact: true
      location: ".cp/"
      version: "?"
    - class: "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"
      method: "runTests"
      file: "RemoteTestRunner.java"
      line: 459
      exact: true
      location: ".cp/"
      version: "?"
    - class: "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"
      method: "runTests"
      file: "RemoteTestRunner.java"
      line: 675
      exact: true
      location: ".cp/"
      version: "?"
    - class: "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"
      method: "run"
      file: "RemoteTestRunner.java"
      line: 382
      exact: true
      location: ".cp/"
      version: "?"
    - class: "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"
      method: "main"
      file: "RemoteTestRunner.java"
      line: 192
      exact: true
      location: ".cp/"
      version: "?"
  - commonElementCount: 0
    localizedMessage: "I am suppressed exception 2"
    message: "I am suppressed exception 2"
    name: "java.lang.IndexOutOfBoundsException"
    extendedStackTrace:
    - class: "org.apache.logging.log4j.core.layout.LogEventFixtures"
      method: "createLogEvent"
      file: "LogEventFixtures.java"
      line: 57
      exact: true
      location: "test-classes/"
      version: "?"
    - class: "org.apache.logging.log4j.core.layout.YamlLayoutTest"
      method: "testAllFeatures"
      file: "YamlLayoutTest.java"
      line: 109
      exact: true
      location: "test-classes/"
      version: "?"
    - class: "org.apache.logging.log4j.core.layout.YamlLayoutTest"
      method: "testLocationOnCompactOffEventEolOffMdcOn"
      file: "YamlLayoutTest.java"
      line: 280
      exact: true
      location: "test-classes/"
      version: "?"
    - class: "sun.reflect.NativeMethodAccessorImpl"
      method: "invoke0"
      file: "NativeMethodAccessorImpl.java"
      line: -2
      exact: false
      location: "?"
      version: "1.7.0_79"
    - class: "sun.reflect.NativeMethodAccessorImpl"
      method: "invoke"
      file: "NativeMethodAccessorImpl.java"
      line: 57
      exact: false
      location: "?"
      version: "1.7.0_79"
    - class: "sun.reflect.DelegatingMethodAccessorImpl"
      method: "invoke"
      file: "DelegatingMethodAccessorImpl.java"
      line: 43
      exact: false
      location: "?"
      version: "1.7.0_79"
    - class: "java.lang.reflect.Method"
      method: "invoke"
      file: "Method.java"
      line: 606
      exact: false
      location: "?"
      version: "1.7.0_79"
    - class: "org.junit.runners.model.FrameworkMethod$1"
      method: "runReflectiveCall"
      file: "FrameworkMethod.java"
      line: 50
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.internal.runners.model.ReflectiveCallable"
      method: "run"
      file: "ReflectiveCallable.java"
      line: 12
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.model.FrameworkMethod"
      method: "invokeExplosively"
      file: "FrameworkMethod.java"
      line: 47
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.internal.runners.statements.InvokeMethod"
      method: "evaluate"
      file: "InvokeMethod.java"
      line: 17
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner"
      method: "runLeaf"
      file: "ParentRunner.java"
      line: 325
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.BlockJUnit4ClassRunner"
      method: "runChild"
      file: "BlockJUnit4ClassRunner.java"
      line: 78
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.BlockJUnit4ClassRunner"
      method: "runChild"
      file: "BlockJUnit4ClassRunner.java"
      line: 57
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner$3"
      method: "run"
      file: "ParentRunner.java"
      line: 290
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner$1"
      method: "schedule"
      file: "ParentRunner.java"
      line: 71
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner"
      method: "runChildren"
      file: "ParentRunner.java"
      line: 288
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner"
      method: "access$000"
      file: "ParentRunner.java"
      line: 58
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner$2"
      method: "evaluate"
      file: "ParentRunner.java"
      line: 268
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.internal.runners.statements.RunBefores"
      method: "evaluate"
      file: "RunBefores.java"
      line: 26
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.internal.runners.statements.RunAfters"
      method: "evaluate"
      file: "RunAfters.java"
      line: 27
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.junit.runners.ParentRunner"
      method: "run"
      file: "ParentRunner.java"
      line: 363
      exact: true
      location: "junit-4.12.jar"
      version: "4.12"
    - class: "org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference"
      method: "run"
      file: "JUnit4TestReference.java"
      line: 86
      exact: true
      location: ".cp/"
      version: "?"
    - class: "org.eclipse.jdt.internal.junit.runner.TestExecution"
      method: "run"
      file: "TestExecution.java"
      line: 38
      exact: true
      location: ".cp/"
      version: "?"
    - class: "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"
      method: "runTests"
      file: "RemoteTestRunner.java"
      line: 459
      exact: true
      location: ".cp/"
      version: "?"
    - class: "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"
      method: "runTests"
      file: "RemoteTestRunner.java"
      line: 675
      exact: true
      location: ".cp/"
      version: "?"
    - class: "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"
      method: "run"
      file: "RemoteTestRunner.java"
      line: 382
      exact: true
      location: ".cp/"
      version: "?"
    - class: "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"
      method: "main"
      file: "RemoteTestRunner.java"
      line: 192
      exact: true
      location: ".cp/"
      version: "?"
contextStack:
- "stack_msg1"
- "stack_msg2"
endOfBatch: false
loggerFqcn: "f.q.c.n"
contextMap:
 MDC.B: "B_Value"
 MDC.A: "A_Value"
threadId: 1
threadPriority: 5
source:
  class: "org.apache.logging.log4j.core.layout.LogEventFixtures"
  method: "createLogEvent"
  file: "LogEventFixtures.java"
  line: 53</pre>
 * <p>
 * This approach enforces the independence of the YamlLayout and the appender where you embed it.
 * </p>
 * <h3>Encoding</h3>
 * <p>
 * Appenders using this layout should have their {@code charset} set to {@code UTF-8} or {@code UTF-16}, otherwise
 * events containing non ASCII characters could result in corrupted log files.
 * </p>
 */
@Plugin(name = "YamlLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class YamlLayout extends AbstractJacksonLayout {

    private static final String DEFAULT_FOOTER = Strings.EMPTY;

    private static final String DEFAULT_HEADER = Strings.EMPTY;

    static final String CONTENT_TYPE = "application/yaml";

    protected YamlLayout(final Configuration config, final boolean locationInfo, final boolean properties,
            final boolean complete, final boolean compact, final boolean eventEol, final String headerPattern,
            final String footerPattern, final Charset charset, final boolean includeStacktrace) {
        super(config, new JacksonFactory.YAML(includeStacktrace).newWriter(locationInfo, properties, compact), charset, compact,
                complete, eventEol,
                PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(headerPattern).setDefaultPattern(DEFAULT_HEADER).build(),
                PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(footerPattern).setDefaultPattern(DEFAULT_FOOTER).build());
    }

    /**
     * Returns appropriate YAML header.
     *
     * @return a byte array containing the header, opening the YAML array.
     */
    @Override
    public byte[] getHeader() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        final String str = serializeToString(getHeaderSerializer());
        if (str != null) {
            buf.append(str);
        }
        buf.append(this.eol);
        return getBytes(buf.toString());
    }

    /**
     * Returns appropriate YAML footer.
     *
     * @return a byte array containing the footer, closing the YAML array.
     */
    @Override
    public byte[] getFooter() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(this.eol);
        final String str = serializeToString(getFooterSerializer());
        if (str != null) {
            buf.append(str);
        }
        buf.append(this.eol);
        return getBytes(buf.toString());
    }

    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>();
        result.put("version", "2.0");
        return result;
    }

    @Override
    /**
     * @return The content type.
     */
    public String getContentType() {
        return CONTENT_TYPE + "; charset=" + this.getCharset();
    }

    /**
     * Creates a YAML Layout.
     * 
     * @param config
     *            The plugin configuration.
     * @param locationInfo
     *            If "true", includes the location information in the generated YAML.
     * @param properties
     *            If "true", includes the thread context map in the generated YAML.
     * @param headerPattern
     *            The header pattern, defaults to {@code ""} if null.
     * @param footerPattern
     *            The header pattern, defaults to {@code ""} if null.
     * @param charset
     *            The character set to use, if {@code null}, uses "UTF-8".
     * @param includeStacktrace
     *            If "true", includes the stacktrace of any Throwable in the generated YAML, defaults to "true".
     * @return A YAML Layout.
     */
    @PluginFactory
    public static AbstractJacksonLayout createLayout(
            // @formatter:off
            @PluginConfiguration final Configuration config,
            @PluginAttribute(value = "locationInfo") final boolean locationInfo,
            @PluginAttribute(value = "properties") final boolean properties,
            @PluginAttribute(value = "header", defaultString = DEFAULT_HEADER) final String headerPattern,
            @PluginAttribute(value = "footer", defaultString = DEFAULT_FOOTER) final String footerPattern,
            @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charset,
            @PluginAttribute(value = "includeStacktrace", defaultBoolean = true) final boolean includeStacktrace
            // @formatter:on
    ) {
        return new YamlLayout(config, locationInfo, properties, false, false, true, headerPattern, footerPattern,
                charset, includeStacktrace);
    }

    /**
     * Creates a YAML Layout using the default settings. Useful for testing.
     *
     * @return A YAML Layout.
     */
    public static AbstractJacksonLayout createDefaultLayout() {
        return new YamlLayout(new DefaultConfiguration(), false, false, false, false, false, DEFAULT_HEADER,
                DEFAULT_FOOTER, StandardCharsets.UTF_8, true);
    }

    @Override
    public void toSerializable(final LogEvent event, final Writer writer) throws IOException {
        if (complete && eventCount > 0) {
            writer.append(", ");
        }
        super.toSerializable(event, writer);
    }
}
