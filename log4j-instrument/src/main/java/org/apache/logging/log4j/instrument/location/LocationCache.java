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
package org.apache.logging.log4j.instrument.location;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.instrument.Constants;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import static org.apache.logging.log4j.instrument.Constants.OBJECT_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STACK_TRACE_ELEMENT_ARRAY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STACK_TRACE_ELEMENT_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STRING_TYPE;

public class LocationCache {

    private static final String LOCATION_FIELD = "locations";

    private final Map<String, List<StackTraceElement>> locations = new ConcurrentHashMap<>();

    public LocationCacheValue addLocation(final String internalClassName, final String methodName, final String fileName,
            final int lineNumber) {
        final String cacheClassName = getCacheClassName(internalClassName);
        final List<StackTraceElement> perClassLocations = locations.computeIfAbsent(cacheClassName,
                k -> new CopyOnWriteArrayList<>());
        final StackTraceElement location = new StackTraceElement(internalClassName.replaceAll("/", "."), methodName,
                fileName, lineNumber);
        perClassLocations.add(location);
        return new LocationCacheValue(cacheClassName, LOCATION_FIELD, perClassLocations.indexOf(location));
    }

    public Map<String, byte[]> generateClasses() {
        return locations.entrySet()
                .parallelStream()
                .collect(Collectors.toMap(Entry::getKey, e -> generateCacheClass(e.getKey(), e.getValue())));
    }

    private static byte[] generateCacheClass(final String innerClassName, final List<StackTraceElement> locations) {
        final ClassWriter cv = new ClassWriter(0);
        cv.visit(Opcodes.V1_8, 0, innerClassName, null, OBJECT_TYPE.getInternalName(), null);
        cv.visitField(Opcodes.ACC_STATIC, LOCATION_FIELD, STACK_TRACE_ELEMENT_ARRAY_TYPE.getInternalName(), null, null)
                .visitEnd();
        final InstructionAdapter mv = new InstructionAdapter(
                cv.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null));
        mv.visitCode();
        mv.visitMaxs(9, 0);
        mv.iconst(locations.size());
        mv.newarray(STACK_TRACE_ELEMENT_TYPE);
        for (int i = 0; i < locations.size(); i++) {
            final StackTraceElement location = locations.get(i);
            mv.dup();
            mv.iconst(i);
            mv.anew(STACK_TRACE_ELEMENT_TYPE);
            mv.dup();
            mv.aconst(location.getClassName());
            mv.aconst(location.getMethodName());
            mv.aconst(location.getFileName());
            mv.iconst(location.getLineNumber());
            mv.invokespecial(STACK_TRACE_ELEMENT_TYPE.getInternalName(), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, STRING_TYPE, STRING_TYPE, STRING_TYPE, Type.INT_TYPE),
                    false);
            mv.visitInsn(Opcodes.AASTORE);
        }
        mv.putstatic(innerClassName, LOCATION_FIELD, STACK_TRACE_ELEMENT_ARRAY_TYPE.getInternalName());
        mv.areturn(Type.VOID_TYPE);
        mv.visitEnd();
        cv.visitEnd();
        return cv.toByteArray();
    }

    private static String getCacheClassName(final String internalClassName) {
        return StringUtils.substringBefore(internalClassName, '$') + Constants.LOCATION_CACHE_SUFFIX;
    }

    public static Path getCacheClassFile(final Path classFile) {
        final String fileName = classFile.getFileName().toString();
        final String cacheFileName = LocationCache.getCacheClassName(StringUtils.removeEnd(fileName, ".class"))
                + ".class";
        return classFile.resolveSibling(cacheFileName);
    }

    public static class LocationCacheValue {
        private final String internalClassName;
        private final String fieldName;
        private final int index;

        private LocationCacheValue(String internalClassName, String fieldName, int index) {
            super();
            this.internalClassName = internalClassName;
            this.fieldName = fieldName;
            this.index = index;
        }

        public String getInternalClassName() {
            return internalClassName;
        }

        public Type getType() {
            return Type.getObjectType(internalClassName);
        }

        public String getFieldName() {
            return fieldName;
        }

        public int getIndex() {
            return index;
        }

    }
}
