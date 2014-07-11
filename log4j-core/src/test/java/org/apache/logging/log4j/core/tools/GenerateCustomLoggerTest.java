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

package org.apache.logging.log4j.core.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.TestLogger;
import org.apache.logging.log4j.core.tools.Generate;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class GenerateCustomLoggerTest {
    
    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.TestLoggerContextFactory");
    }

    @Test
    public void testGenerateSource() throws Exception {
        final String CLASSNAME = "org.myorg.MyCustomLogger";

        // generate custom logger source
        List<String> values = Arrays.asList("DEFCON1=350 DEFCON2=450 DEFCON3=550".split(" "));
        List<Generate.LevelInfo> levels = Generate.LevelInfo.parse(values, Generate.CustomLogger.class);
        String src = Generate.generateSource(CLASSNAME, levels, Generate.Type.CUSTOM);
        File f = new File("target/test-classes/org/myorg/MyCustomLogger.java");
        f.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(f);
        out.write(src.getBytes(Charset.defaultCharset()));
        out.close();

        // set up compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(f));

        // compile generated source
        compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits).call();

        // check we don't have any compilation errors
        List<String> errors = new ArrayList<String>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errors.add(String.format("Compile error: %s%n", diagnostic.getMessage(Locale.getDefault())));
            }
        }
        fileManager.close();
        assertTrue(errors.toString(), errors.isEmpty());

        // load the compiled class
        Class<?> cls = Class.forName(CLASSNAME);

        // check that all factory methods exist and are static
        assertTrue(Modifier.isStatic(cls.getDeclaredMethod("create", new Class[0]).getModifiers()));
        assertTrue(Modifier.isStatic(cls.getDeclaredMethod("create", new Class[] { Class.class }).getModifiers()));
        assertTrue(Modifier.isStatic(cls.getDeclaredMethod("create", new Class[] { Object.class }).getModifiers()));
        assertTrue(Modifier.isStatic(cls.getDeclaredMethod("create", new Class[] { String.class }).getModifiers()));
        assertTrue(Modifier.isStatic(cls.getDeclaredMethod("create", Class.class, MessageFactory.class).getModifiers()));
        assertTrue(Modifier
                .isStatic(cls.getDeclaredMethod("create", Object.class, MessageFactory.class).getModifiers()));
        assertTrue(Modifier
                .isStatic(cls.getDeclaredMethod("create", String.class, MessageFactory.class).getModifiers()));

        // check that all log methods exist
        String[] logMethods = { "defcon1", "defcon2", "defcon3" };
        for (String name : logMethods) {
            cls.getDeclaredMethod(name, Marker.class, Message.class, Throwable.class);
            cls.getDeclaredMethod(name, Marker.class, Object.class, Throwable.class);
            cls.getDeclaredMethod(name, Marker.class, String.class, Throwable.class);
            cls.getDeclaredMethod(name, Marker.class, Message.class);
            cls.getDeclaredMethod(name, Marker.class, Object.class);
            cls.getDeclaredMethod(name, Marker.class, String.class);
            cls.getDeclaredMethod(name, Message.class);
            cls.getDeclaredMethod(name, Object.class);
            cls.getDeclaredMethod(name, String.class);
            cls.getDeclaredMethod(name, Message.class, Throwable.class);
            cls.getDeclaredMethod(name, Object.class, Throwable.class);
            cls.getDeclaredMethod(name, String.class, Throwable.class);
            cls.getDeclaredMethod(name, String.class, Object[].class);
            cls.getDeclaredMethod(name, Marker.class, String.class, Object[].class);
        }

        // now see if it actually works...
        Method create = cls.getDeclaredMethod("create", new Class[] { String.class });
        Object customLogger = create.invoke(null, "X.Y.Z");
        int n = 0;
        for (String name : logMethods) {
            Method method = cls.getDeclaredMethod(name, String.class);
            method.invoke(customLogger, "This is message " + n++);
        }

        TestLogger underlying = (TestLogger) LogManager.getLogger("X.Y.Z");
        List<String> lines = underlying.getEntries();
        for (int i = 0; i < lines.size(); i++) {
            assertEquals(" " + levels.get(i).name + " This is message " + i, lines.get(i));
        }
    }
}
