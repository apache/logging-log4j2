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
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Charsets;

/**
 * Appends a series of JSON events as strings serialized as bytes.
 *
 * <h4>Complete well-formed JSON vs. fragment JSON</h4>
 * <p>
 * If you configure {@code complete="true"}, the appender outputs a well-formed JSON document. By default, with {@code complete="false"},
 * you should include the output as an <em>external file</em> in a separate file to form a well-formed JSON document.
 * </p>
 * <p>
 * A well-formed JSON event follows this pattern:
 * </p>
 *
 * <pre>
 * {
  "timeMillis": 1,
  "thread": "MyThreadName",
  "level": "DEBUG",
  "loggerName": "a.B",
  "marker": {
    "name": "Marker1",
    "parents": [{
      "name": "ParentMarker1",
      "parents": [{
        "name": "GrandMotherMarker"
      }, {
        "name": "GrandFatherMarker"
      }]
    }, {
      "name": "GrandFatherMarker"
    }]
  },
  "message": "Msg",
  "thrown": {
    "cause": {
      "commonElementCount": 27,
      "extendedStackTrace": [{
        "class": "org.apache.logging.log4j.core.layout.LogEventFixtures",
        "method": "createLogEvent",
        "file": "LogEventFixtures.java",
        "line": 53,
        "exact": false,
        "location": "test-classes/",
        "version": "?"
      }],
      "localizedMessage": "testNPEx",
      "message": "testNPEx",
      "name": "java.lang.NullPointerException"
    },
    "commonElementCount": 0,
    "extendedStackTrace": [{
      "class": "org.apache.logging.log4j.core.layout.LogEventFixtures",
      "method": "createLogEvent",
      "file": "LogEventFixtures.java",
      "line": 56,
      "exact": true,
      "location": "test-classes/",
      "version": "?"
    }, {
      "class": "org.apache.logging.log4j.core.layout.JsonLayoutTest",
      "method": "testAllFeatures",
      "file": "JsonLayoutTest.java",
      "line": 105,
      "exact": true,
      "location": "test-classes/",
      "version": "?"
    }, {
      "class": "org.apache.logging.log4j.core.layout.JsonLayoutTest",
      "method": "testLocationOnCompactOnMdcOn",
      "file": "JsonLayoutTest.java",
      "line": 268,
      "exact": true,
      "location": "test-classes/",
      "version": "?"
    }, {
      "class": "sun.reflect.NativeMethodAccessorImpl",
      "method": "invoke",
      "line": -1,
      "exact": false,
      "location": "?",
      "version": "1.7.0_55"
    }, {
      "class": "sun.reflect.NativeMethodAccessorImpl",
      "method": "invoke",
      "line": -1,
      "exact": false,
      "location": "?",
      "version": "1.7.0_55"
    }, {
      "class": "sun.reflect.DelegatingMethodAccessorImpl",
      "method": "invoke",
      "line": -1,
      "exact": false,
      "location": "?",
      "version": "1.7.0_55"
    }, {
      "class": "java.lang.reflect.Method",
      "method": "invoke",
      "line": -1,
      "exact": false,
      "location": "?",
      "version": "1.7.0_55"
    }, {
      "class": "org.junit.runners.model.FrameworkMethod$1",
      "method": "runReflectiveCall",
      "file": "FrameworkMethod.java",
      "line": 47,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.internal.runners.model.ReflectiveCallable",
      "method": "run",
      "file": "ReflectiveCallable.java",
      "line": 12,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.runners.model.FrameworkMethod",
      "method": "invokeExplosively",
      "file": "FrameworkMethod.java",
      "line": 44,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.internal.runners.statements.InvokeMethod",
      "method": "evaluate",
      "file": "InvokeMethod.java",
      "line": 17,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.runners.ParentRunner",
      "method": "runLeaf",
      "file": "ParentRunner.java",
      "line": 271,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.runners.BlockJUnit4ClassRunner",
      "method": "runChild",
      "file": "BlockJUnit4ClassRunner.java",
      "line": 70,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.runners.BlockJUnit4ClassRunner",
      "method": "runChild",
      "file": "BlockJUnit4ClassRunner.java",
      "line": 50,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.runners.ParentRunner$3",
      "method": "run",
      "file": "ParentRunner.java",
      "line": 238,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.runners.ParentRunner$1",
      "method": "schedule",
      "file": "ParentRunner.java",
      "line": 63,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.runners.ParentRunner",
      "method": "runChildren",
      "file": "ParentRunner.java",
      "line": 236,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.runners.ParentRunner",
      "method": "access$000",
      "file": "ParentRunner.java",
      "line": 53,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.runners.ParentRunner$2",
      "method": "evaluate",
      "file": "ParentRunner.java",
      "line": 229,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.internal.runners.statements.RunBefores",
      "method": "evaluate",
      "file": "RunBefores.java",
      "line": 26,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.internal.runners.statements.RunAfters",
      "method": "evaluate",
      "file": "RunAfters.java",
      "line": 27,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.junit.runners.ParentRunner",
      "method": "run",
      "file": "ParentRunner.java",
      "line": 309,
      "exact": true,
      "location": "junit-4.11.jar",
      "version": "?"
    }, {
      "class": "org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference",
      "method": "run",
      "file": "JUnit4TestReference.java",
      "line": 50,
      "exact": true,
      "location": ".cp/",
      "version": "?"
    }, {
      "class": "org.eclipse.jdt.internal.junit.runner.TestExecution",
      "method": "run",
      "file": "TestExecution.java",
      "line": 38,
      "exact": true,
      "location": ".cp/",
      "version": "?"
    }, {
      "class": "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
      "method": "runTests",
      "file": "RemoteTestRunner.java",
      "line": 467,
      "exact": true,
      "location": ".cp/",
      "version": "?"
    }, {
      "class": "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
      "method": "runTests",
      "file": "RemoteTestRunner.java",
      "line": 683,
      "exact": true,
      "location": ".cp/",
      "version": "?"
    }, {
      "class": "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
      "method": "run",
      "file": "RemoteTestRunner.java",
      "line": 390,
      "exact": true,
      "location": ".cp/",
      "version": "?"
    }, {
      "class": "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
      "method": "main",
      "file": "RemoteTestRunner.java",
      "line": 197,
      "exact": true,
      "location": ".cp/",
      "version": "?"
    }],
    "localizedMessage": "testIOEx",
    "message": "testIOEx",
    "name": "java.io.IOException",
    "suppressed": [{
      "commonElementCount": 0,
      "extendedStackTrace": [{
        "class": "org.apache.logging.log4j.core.layout.LogEventFixtures",
        "method": "createLogEvent",
        "file": "LogEventFixtures.java",
        "line": 57,
        "exact": true,
        "location": "test-classes/",
        "version": "?"
      }, {
        "class": "org.apache.logging.log4j.core.layout.JsonLayoutTest",
        "method": "testAllFeatures",
        "file": "JsonLayoutTest.java",
        "line": 105,
        "exact": true,
        "location": "test-classes/",
        "version": "?"
      }, {
        "class": "org.apache.logging.log4j.core.layout.JsonLayoutTest",
        "method": "testLocationOnCompactOnMdcOn",
        "file": "JsonLayoutTest.java",
        "line": 268,
        "exact": true,
        "location": "test-classes/",
        "version": "?"
      }, {
        "class": "sun.reflect.NativeMethodAccessorImpl",
        "method": "invoke",
        "line": -1,
        "exact": false,
        "location": "?",
        "version": "1.7.0_55"
      }, {
        "class": "sun.reflect.NativeMethodAccessorImpl",
        "method": "invoke",
        "line": -1,
        "exact": false,
        "location": "?",
        "version": "1.7.0_55"
      }, {
        "class": "sun.reflect.DelegatingMethodAccessorImpl",
        "method": "invoke",
        "line": -1,
        "exact": false,
        "location": "?",
        "version": "1.7.0_55"
      }, {
        "class": "java.lang.reflect.Method",
        "method": "invoke",
        "line": -1,
        "exact": false,
        "location": "?",
        "version": "1.7.0_55"
      }, {
        "class": "org.junit.runners.model.FrameworkMethod$1",
        "method": "runReflectiveCall",
        "file": "FrameworkMethod.java",
        "line": 47,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.internal.runners.model.ReflectiveCallable",
        "method": "run",
        "file": "ReflectiveCallable.java",
        "line": 12,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.model.FrameworkMethod",
        "method": "invokeExplosively",
        "file": "FrameworkMethod.java",
        "line": 44,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.internal.runners.statements.InvokeMethod",
        "method": "evaluate",
        "file": "InvokeMethod.java",
        "line": 17,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner",
        "method": "runLeaf",
        "file": "ParentRunner.java",
        "line": 271,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.BlockJUnit4ClassRunner",
        "method": "runChild",
        "file": "BlockJUnit4ClassRunner.java",
        "line": 70,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.BlockJUnit4ClassRunner",
        "method": "runChild",
        "file": "BlockJUnit4ClassRunner.java",
        "line": 50,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner$3",
        "method": "run",
        "file": "ParentRunner.java",
        "line": 238,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner$1",
        "method": "schedule",
        "file": "ParentRunner.java",
        "line": 63,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner",
        "method": "runChildren",
        "file": "ParentRunner.java",
        "line": 236,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner",
        "method": "access$000",
        "file": "ParentRunner.java",
        "line": 53,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner$2",
        "method": "evaluate",
        "file": "ParentRunner.java",
        "line": 229,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.internal.runners.statements.RunBefores",
        "method": "evaluate",
        "file": "RunBefores.java",
        "line": 26,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.internal.runners.statements.RunAfters",
        "method": "evaluate",
        "file": "RunAfters.java",
        "line": 27,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner",
        "method": "run",
        "file": "ParentRunner.java",
        "line": 309,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference",
        "method": "run",
        "file": "JUnit4TestReference.java",
        "line": 50,
        "exact": true,
        "location": ".cp/",
        "version": "?"
      }, {
        "class": "org.eclipse.jdt.internal.junit.runner.TestExecution",
        "method": "run",
        "file": "TestExecution.java",
        "line": 38,
        "exact": true,
        "location": ".cp/",
        "version": "?"
      }, {
        "class": "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
        "method": "runTests",
        "file": "RemoteTestRunner.java",
        "line": 467,
        "exact": true,
        "location": ".cp/",
        "version": "?"
      }, {
        "class": "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
        "method": "runTests",
        "file": "RemoteTestRunner.java",
        "line": 683,
        "exact": true,
        "location": ".cp/",
        "version": "?"
      }, {
        "class": "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
        "method": "run",
        "file": "RemoteTestRunner.java",
        "line": 390,
        "exact": true,
        "location": ".cp/",
        "version": "?"
      }, {
        "class": "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
        "method": "main",
        "file": "RemoteTestRunner.java",
        "line": 197,
        "exact": true,
        "location": ".cp/",
        "version": "?"
      }],
      "localizedMessage": "I am suppressed exception 1",
      "message": "I am suppressed exception 1",
      "name": "java.lang.IndexOutOfBoundsException"
    }, {
      "commonElementCount": 0,
      "extendedStackTrace": [{
        "class": "org.apache.logging.log4j.core.layout.LogEventFixtures",
        "method": "createLogEvent",
        "file": "LogEventFixtures.java",
        "line": 58,
        "exact": true,
        "location": "test-classes/",
        "version": "?"
      }, {
        "class": "org.apache.logging.log4j.core.layout.JsonLayoutTest",
        "method": "testAllFeatures",
        "file": "JsonLayoutTest.java",
        "line": 105,
        "exact": true,
        "location": "test-classes/",
        "version": "?"
      }, {
        "class": "org.apache.logging.log4j.core.layout.JsonLayoutTest",
        "method": "testLocationOnCompactOnMdcOn",
        "file": "JsonLayoutTest.java",
        "line": 268,
        "exact": true,
        "location": "test-classes/",
        "version": "?"
      }, {
        "class": "sun.reflect.NativeMethodAccessorImpl",
        "method": "invoke",
        "line": -1,
        "exact": false,
        "location": "?",
        "version": "1.7.0_55"
      }, {
        "class": "sun.reflect.NativeMethodAccessorImpl",
        "method": "invoke",
        "line": -1,
        "exact": false,
        "location": "?",
        "version": "1.7.0_55"
      }, {
        "class": "sun.reflect.DelegatingMethodAccessorImpl",
        "method": "invoke",
        "line": -1,
        "exact": false,
        "location": "?",
        "version": "1.7.0_55"
      }, {
        "class": "java.lang.reflect.Method",
        "method": "invoke",
        "line": -1,
        "exact": false,
        "location": "?",
        "version": "1.7.0_55"
      }, {
        "class": "org.junit.runners.model.FrameworkMethod$1",
        "method": "runReflectiveCall",
        "file": "FrameworkMethod.java",
        "line": 47,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.internal.runners.model.ReflectiveCallable",
        "method": "run",
        "file": "ReflectiveCallable.java",
        "line": 12,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.model.FrameworkMethod",
        "method": "invokeExplosively",
        "file": "FrameworkMethod.java",
        "line": 44,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.internal.runners.statements.InvokeMethod",
        "method": "evaluate",
        "file": "InvokeMethod.java",
        "line": 17,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner",
        "method": "runLeaf",
        "file": "ParentRunner.java",
        "line": 271,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.BlockJUnit4ClassRunner",
        "method": "runChild",
        "file": "BlockJUnit4ClassRunner.java",
        "line": 70,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.BlockJUnit4ClassRunner",
        "method": "runChild",
        "file": "BlockJUnit4ClassRunner.java",
        "line": 50,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner$3",
        "method": "run",
        "file": "ParentRunner.java",
        "line": 238,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner$1",
        "method": "schedule",
        "file": "ParentRunner.java",
        "line": 63,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner",
        "method": "runChildren",
        "file": "ParentRunner.java",
        "line": 236,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner",
        "method": "access$000",
        "file": "ParentRunner.java",
        "line": 53,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner$2",
        "method": "evaluate",
        "file": "ParentRunner.java",
        "line": 229,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.internal.runners.statements.RunBefores",
        "method": "evaluate",
        "file": "RunBefores.java",
        "line": 26,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.internal.runners.statements.RunAfters",
        "method": "evaluate",
        "file": "RunAfters.java",
        "line": 27,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.junit.runners.ParentRunner",
        "method": "run",
        "file": "ParentRunner.java",
        "line": 309,
        "exact": true,
        "location": "junit-4.11.jar",
        "version": "?"
      }, {
        "class": "org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference",
        "method": "run",
        "file": "JUnit4TestReference.java",
        "line": 50,
        "exact": true,
        "location": ".cp/",
        "version": "?"
      }, {
        "class": "org.eclipse.jdt.internal.junit.runner.TestExecution",
        "method": "run",
        "file": "TestExecution.java",
        "line": 38,
        "exact": true,
        "location": ".cp/",
        "version": "?"
      }, {
        "class": "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
        "method": "runTests",
        "file": "RemoteTestRunner.java",
        "line": 467,
        "exact": true,
        "location": ".cp/",
        "version": "?"
      }, {
        "class": "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
        "method": "runTests",
        "file": "RemoteTestRunner.java",
        "line": 683,
        "exact": true,
        "location": ".cp/",
        "version": "?"
      }, {
        "class": "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
        "method": "run",
        "file": "RemoteTestRunner.java",
        "line": 390,
        "exact": true,
        "location": ".cp/",
        "version": "?"
      }, {
        "class": "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
        "method": "main",
        "file": "RemoteTestRunner.java",
        "line": 197,
        "exact": true,
        "location": ".cp/",
        "version": "?"
      }],
      "localizedMessage": "I am suppressed exception 2",
      "message": "I am suppressed exception 2",
      "name": "java.lang.IndexOutOfBoundsException"
    }]
  },
  "loggerFQCN": "f.q.c.n",
  "endOfBatch": false,
  "contextMap": [{
    "key": "MDC.B",
    "value": "B_Value"
  }, {
    "key": "MDC.A",
    "value": "A_Value"
  }],
  "contextStack": ["stack_msg1", "stack_msg2"],
  "source": {
    "class": "org.apache.logging.log4j.core.layout.LogEventFixtures",
    "method": "createLogEvent",
    "file": "LogEventFixtures.java",
    "line": 54
  }
}
 * </pre>
 * <p>
 * If {@code complete="false"}, the appender does not write the JSON open array character "[" at the start of the document. and "]" and the
 * end.
 * </p>
 * <p>
 * This approach enforces the independence of the JsonLayout and the appender where you embed it.
 * </p>
 * <h4>Encoding</h4>
 * <p>
 * Appenders using this layout should have their {@code charset} set to {@code UTF-8} or {@code UTF-16}, otherwise events containing non
 * ASCII characters could result in corrupted log files.
 * </p>
 * <h4>Pretty vs. compact XML</h4>
 * <p>
 * By default, the JSON layout is not compact (a.k.a. not "pretty") with {@code compact="false"}, which means the appender uses end-of-line
 * characters and indents lines to format the text. If {@code compact="true"}, then no end-of-line or indentation is used. Message content
 * may contain, of course, escaped end-of-lines.
 * </p>
 */
@Plugin(name = "JsonLayout", category = "Core", elementType = "layout", printObject = true)
public final class JsonLayout extends AbstractJacksonLayout {

    static final String CONTENT_TYPE = "application/json";
    
    private static final long serialVersionUID = 1L;

    protected JsonLayout(final boolean locationInfo, final boolean properties, final boolean complete, final boolean compact,
            final Charset charset) {
        super(new JacksonFactory.JSON().newWriter(locationInfo, properties, compact), charset, compact, complete);
    }

    /**
     * Returns appropriate JSON headers.
     *
     * @return a byte array containing the header, opening the JSON array.
     */
    @Override
    public byte[] getHeader() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        buf.append(this.eol);
        return buf.toString().getBytes(this.getCharset());
    }

    /**
     * Returns appropriate JSON footer.
     *
     * @return a byte array containing the footer, closing the JSON array.
     */
    @Override
    public byte[] getFooter() {
        if (!this.complete) {
            return null;
        }
        return (this.eol + ']' + this.eol).getBytes(this.getCharset());
    }

    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<String, String>();
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
     * Creates a JSON Layout.
     *
     * @param locationInfo If "true", includes the location information in the generated JSON.
     * @param properties If "true", includes the thread context in the generated JSON.
     * @param complete If "true", includes the JSON header and footer, defaults to "false".
     * @param compact If "true", does not use end-of-lines and indentation, defaults to "false".
     * @param charset The character set to use, if {@code null}, uses "UTF-8".
     * @return A JSON Layout.
     */
    @PluginFactory
    public static AbstractJacksonLayout createLayout(
            // @formatter:off
            @PluginAttribute(value = "locationInfo", defaultBoolean = false) final boolean locationInfo,
            @PluginAttribute(value = "properties", defaultBoolean = false) final boolean properties,
            @PluginAttribute(value = "complete", defaultBoolean = false) final boolean complete,
            @PluginAttribute(value = "compact", defaultBoolean = false) final boolean compact,
            @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charset
            // @formatter:on
    ) {
        return new JsonLayout(locationInfo, properties, complete, compact, charset);
    }

    /**
     * Creates a JSON Layout using the default settings.
     *
     * @return A JSON Layout.
     */
    public static AbstractJacksonLayout createDefaultLayout() {
        return new JsonLayout(false, false, false, false, Charsets.UTF_8);
    }
}
