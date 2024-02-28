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
package org.apache.logging.log4j.kit.logger;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Set;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.MethodInfo;
import org.junit.jupiter.api.Test;

public class AbstractLoggerTest {

    private static final int MAX_INLINE_SIZE = 35;
    /**
     * List of methods that currently don't fit into 35 bytes.
     */
    private static final Set<String> NOT_INLINED_METHOD_PREFIXES = Set.of(
            "<clinit>()V",
            "<init>(",
            "handleLogMessageException(Ljava/lang/Throwable;Ljava/lang/String;Lorg/apache/logging/log4j/message/Message;)V",
            // logging methods with Supplier, MessageSupplier or more than 2 parameters
            "logMessage(Ljava/lang/String;Lorg/apache/logging/log4j/Level;Lorg/apache/logging/log4j/Marker;Lorg/apache/logging/log4j/util/MessageSupplier;Ljava/lang/Throwable;)V",
            "logMessage(Ljava/lang/String;Lorg/apache/logging/log4j/Level;Lorg/apache/logging/log4j/Marker;Lorg/apache/logging/log4j/util/Supplier;Ljava/lang/Throwable;)V",
            "logMessage(Ljava/lang/String;Lorg/apache/logging/log4j/Level;Lorg/apache/logging/log4j/Marker;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;",
            "logMessage(Ljava/lang/String;Lorg/apache/logging/log4j/Level;Lorg/apache/logging/log4j/Marker;Ljava/lang/String;[Lorg/apache/logging/log4j/util/Supplier;)V",
            // logging methods with more than 3 parameters
            "logIfEnabled(Ljava/lang/String;Lorg/apache/logging/log4j/Level;Lorg/apache/logging/log4j/Marker;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;");

    @Test
    void does_not_exceed_MaxInlineSize() throws Exception {
        try (final InputStream is = AbstractLoggerTest.class.getResourceAsStream("AbstractLogger.class")) {
            final ClassFile classFile = new ClassFile(new DataInputStream(is));
            for (final MethodInfo methodInfo : classFile.getMethods()) {
                final String key = methodInfo.getName() + methodInfo.getDescriptor();
                final CodeAttribute code = methodInfo.getCodeAttribute();
                if (code != null && shouldBeInlined(key)) {
                    assertThat(code.getCodeLength())
                            .as("Method %s is within MaxInlineSize threshold.", key)
                            .isLessThanOrEqualTo(MAX_INLINE_SIZE);
                }
            }
        }
    }

    private static boolean shouldBeInlined(final String key) {
        for (final String prefix : NOT_INLINED_METHOD_PREFIXES) {
            if (key.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }
}
