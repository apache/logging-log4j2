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
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.SyslogLayout.Builder;

/**
 * Appends a series of JSON events as strings serialized as bytes.
 *
 * <h3>Complete well-formed JSON vs. fragment JSON</h3>
 * <p>
 * If you configure {@code complete="true"}, the appender outputs a well-formed JSON document. By default, with
 * {@code complete="false"}, you should include the output as an <em>external file</em> in a separate file to form a
 * well-formed JSON document.
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
 * If {@code complete="false"}, the appender does not write the JSON open array character "[" at the start
 * of the document, "]" and the end, nor comma "," between records.
 * </p>
 * <p>
 * This approach enforces the independence of the JsonLayout and the appender where you embed it.
 * </p>
 * <h3>Encoding</h3>
 * <p>
 * Appenders using this layout should have their {@code charset} set to {@code UTF-8} or {@code UTF-16}, otherwise
 * events containing non ASCII characters could result in corrupted log files.
 * </p>
 * <h3>Pretty vs. compact XML</h3>
 * <p>
 * By default, the JSON layout is not compact (a.k.a. "pretty") with {@code compact="false"}, which means the
 * appender uses end-of-line characters and indents lines to format the text. If {@code compact="true"}, then no
 * end-of-line or indentation is used. Message content may contain, of course, escaped end-of-lines.
 * </p>
 */
@Plugin(name = "JsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class JsonLayout extends AbstractJacksonLayout {

    private static final String DEFAULT_FOOTER = "]";

    private static final String DEFAULT_HEADER = "[";

    static final String CONTENT_TYPE = "application/json";

    public static class Builder<B extends Builder<B>> extends AbstractJacksonLayout.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<JsonLayout> {

        @PluginBuilderAttribute
        private boolean locationInfo;
        
        @PluginBuilderAttribute
        private boolean properties;
        
        @PluginBuilderAttribute
        private boolean propertiesAsList;
        
        @PluginBuilderAttribute
        private boolean includeStacktrace = true;

        public Builder() {
            super();
            setCharset(StandardCharsets.UTF_8);
        }

        @Override
        public JsonLayout build() {
            final boolean encodeThreadContextAsList = properties && propertiesAsList;
            final String headerPattern = toStringOrNull(getHeader());
            final String footerPattern = toStringOrNull(getFooter());
            return new JsonLayout(getConfiguration(), locationInfo, properties, encodeThreadContextAsList, isComplete(),
                    isCompact(), getEventEol(), headerPattern, footerPattern, getCharset(), includeStacktrace);
        }

        private String toStringOrNull(final byte[] header) {
            return header == null ? null : new String(header, Charset.defaultCharset());
        }

        public boolean isLocationInfo() {
            return locationInfo;
        }

        public boolean isProperties() {
            return properties;
        }

        public boolean isPropertiesAsList() {
            return propertiesAsList;
        }

        /**
         * If "true", includes the stacktrace of any Throwable in the generated JSON, defaults to "true".
         * @return If "true", includes the stacktrace of any Throwable in the generated JSON, defaults to "true".
         */
        public boolean isIncludeStacktrace() {
            return includeStacktrace;
        }

        public B setLocationInfo(boolean locationInfo) {
            this.locationInfo = locationInfo;
            return asBuilder();
        }

        public B setProperties(boolean properties) {
            this.properties = properties;
            return asBuilder();
        }

        public B setPropertiesAsList(boolean propertiesAsList) {
            this.propertiesAsList = propertiesAsList;
            return asBuilder();
        }

        /**
         * If "true", includes the stacktrace of any Throwable in the generated JSON, defaults to "true".
         * @param includeStacktrace If "true", includes the stacktrace of any Throwable in the generated JSON, defaults to "true".
         * @return this builder
         */
        public B setIncludeStacktrace(boolean includeStacktrace) {
            this.includeStacktrace = includeStacktrace;
            return asBuilder();
        }
    }

    protected JsonLayout(final Configuration config, final boolean locationInfo, final boolean properties,
            final boolean encodeThreadContextAsList,
            final boolean complete, final boolean compact, final boolean eventEol, final String headerPattern,
            final String footerPattern, final Charset charset, final boolean includeStacktrace) {
        super(config, new JacksonFactory.JSON(encodeThreadContextAsList, includeStacktrace).newWriter(
                    locationInfo, properties, compact),
                charset, compact, complete, eventEol,
                PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(headerPattern).setDefaultPattern(DEFAULT_HEADER).build(),
                PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(footerPattern).setDefaultPattern(DEFAULT_FOOTER).build());
    }

    /**
     * Returns appropriate JSON header.
     *
     * @return a byte array containing the header, opening the JSON array.
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
     * Returns appropriate JSON footer.
     *
     * @return a byte array containing the footer, closing the JSON array.
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
     * Creates a JSON Layout.
     * @param config
     *           The plugin configuration.
     * @param locationInfo
     *            If "true", includes the location information in the generated JSON.
     * @param properties
     *            If "true", includes the thread context map in the generated JSON.
     * @param propertiesAsList
     *            If true, the thread context map is included as a list of map entry objects, where each entry has
     *            a "key" attribute (whose value is the key) and a "value" attribute (whose value is the value).
     *            Defaults to false, in which case the thread context map is included as a simple map of key-value
     *            pairs.
     * @param complete
     *            If "true", includes the JSON header and footer, and comma between records.
     * @param compact
     *            If "true", does not use end-of-lines and indentation, defaults to "false".
     * @param eventEol
     *            If "true", forces an EOL after each log event (even if compact is "true"), defaults to "false". This
     *            allows one even per line, even in compact mode.
     * @param headerPattern
     *            The header pattern, defaults to {@code "["} if null.
     * @param footerPattern
     *            The header pattern, defaults to {@code "]"} if null.
     * @param charset
     *            The character set to use, if {@code null}, uses "UTF-8".
     * @param includeStacktrace
     *            If "true", includes the stacktrace of any Throwable in the generated JSON, defaults to "true".
     * @return A JSON Layout.
     */
    @Deprecated
    public static JsonLayout createLayout(
            // @formatter:off
            @PluginConfiguration final Configuration config,
            @PluginAttribute(value = "locationInfo") final boolean locationInfo,
            @PluginAttribute(value = "properties") final boolean properties,
            @PluginAttribute(value = "propertiesAsList") final boolean propertiesAsList,
            @PluginAttribute(value = "complete") final boolean complete,
            @PluginAttribute(value = "compact") final boolean compact,
            @PluginAttribute(value = "eventEol") final boolean eventEol,
            @PluginAttribute(value = "header", defaultString = DEFAULT_HEADER) final String headerPattern,
            @PluginAttribute(value = "footer", defaultString = DEFAULT_FOOTER) final String footerPattern,
            @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charset,
            @PluginAttribute(value = "includeStacktrace", defaultBoolean = true) final boolean includeStacktrace
            // @formatter:on
    ) {
        final boolean encodeThreadContextAsList = properties && propertiesAsList;
        return new JsonLayout(config, locationInfo, properties, encodeThreadContextAsList, complete, compact, eventEol,
                headerPattern, footerPattern, charset, includeStacktrace);
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    /**
     * Creates a JSON Layout using the default settings. Useful for testing.
     *
     * @return A JSON Layout.
     */
    public static JsonLayout createDefaultLayout() {
        return new JsonLayout(new DefaultConfiguration(), false, false, false, false, false, false,
                DEFAULT_HEADER, DEFAULT_FOOTER, StandardCharsets.UTF_8, true);
    }

    @Override
    public void toSerializable(final LogEvent event, final Writer writer) throws IOException {
        if (complete && eventCount > 0) {
            writer.append(", ");
        }
        super.toSerializable(event, writer);
    }
}
