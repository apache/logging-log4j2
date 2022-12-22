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
package org.apache.logging.log4j.instrument;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import static org.apache.logging.log4j.instrument.Constants.ENTRY_MESSAGE_TYPE;
import static org.apache.logging.log4j.instrument.Constants.FLOW_MESSAGE_FACTORY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.LOGGING_SYSTEM_TYPE;
import static org.apache.logging.log4j.instrument.Constants.MESSAGE_TYPE;
import static org.apache.logging.log4j.instrument.Constants.OBJECT_ARRAY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.OBJECT_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STACK_TRACE_ELEMENT_ARRAY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STACK_TRACE_ELEMENT_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STRING_TYPE;

public class LocationCacheGenerator {

    private static final String CREATE_FORMATTED_MESSAGE_NAME = "createFormattedMessage";
    private static final Type CREATE_FORMATTED_MESSAGE_TYPE = Type.getMethodType(MESSAGE_TYPE, STRING_TYPE,
            OBJECT_ARRAY_TYPE);
    private static final String ENTRY_MESSAGE = "entryMsg";
    private static final Type STRING_FORMATTER_MESSAGE_FACTORY_TYPE = Type
            .getObjectType("org/apache/logging/log4j/message/StringFormatterMessageFactory");
    private static final String LOCATION_FIELD = "locations";

    private final Map<String, LocationCacheContents> locationCacheClasses = new ConcurrentHashMap<>();

    public LocationCacheValue addLocation(final String internalClassName, final String methodName,
            final String fileName, final int lineNumber) {
        final String cacheClassName = getCacheClassName(internalClassName);
        final LocationCacheContents contents = locationCacheClasses.computeIfAbsent(cacheClassName,
                k -> new LocationCacheContents());
        final int index = contents.addLocation(internalClassName, methodName, fileName, lineNumber);
        return new LocationCacheValue(cacheClassName, LOCATION_FIELD, index);
    }

    public Handle createLambda(String internalClassName, SupplierLambdaType type) {
        final String cacheClassName = getCacheClassName(internalClassName);
        final LocationCacheContents contents = locationCacheClasses.computeIfAbsent(cacheClassName,
                k -> new LocationCacheContents());
        contents.addLambda(type);
        switch (type) {
            case FORMATTED_MESSAGE:
                return new Handle(Opcodes.H_INVOKESTATIC, cacheClassName, CREATE_FORMATTED_MESSAGE_NAME,
                        CREATE_FORMATTED_MESSAGE_TYPE.getDescriptor(), false);
            default:
                throw new IllegalArgumentException();
        }
    }

    public Map<String, byte[]> generateClasses() {
        return locationCacheClasses.entrySet()
                .parallelStream()
                .collect(Collectors.toMap(Entry::getKey, e -> generateCacheClass(e.getKey(), e.getValue())));
    }

    private static byte[] generateCacheClass(final String innerClassName, final LocationCacheContents contents) {
        final ClassWriter cv = new ClassWriter(0);
        cv.visit(Opcodes.V1_8, 0, innerClassName, null, OBJECT_TYPE.getInternalName(), null);
        // Write locations field
        final List<StackTraceElement> locations = contents.getLocations();
        writeLocations(innerClassName, cv, locations);
        // We add lambdas to this class
        final Set<SupplierLambdaType> lambdas = contents.getLambdas();
        if (lambdas.contains(SupplierLambdaType.FORMATTED_MESSAGE)) {
            writeCreateStringFormattedMessage(cv);
        }
        cv.visitEnd();
        return cv.toByteArray();
    }

    private static void writeLocations(final String innerClassName, final ClassWriter cv,
            final List<StackTraceElement> locations) {
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
    }

    private static void writeCreateStringFormattedMessage(final ClassWriter cv) {
        final InstructionAdapter mv = new InstructionAdapter(cv.visitMethod(Opcodes.ACC_STATIC,
                CREATE_FORMATTED_MESSAGE_NAME, CREATE_FORMATTED_MESSAGE_TYPE.getDescriptor(), null, null));
        mv.visitCode();
        mv.visitMaxs(3, 2);
        mv.getstatic(STRING_FORMATTER_MESSAGE_FACTORY_TYPE.getInternalName(), "INSTANCE",
                STRING_FORMATTER_MESSAGE_FACTORY_TYPE.getDescriptor());
        mv.load(0, STRING_TYPE);
        mv.load(1, OBJECT_ARRAY_TYPE);
        mv.invokevirtual(STRING_FORMATTER_MESSAGE_FACTORY_TYPE.getInternalName(), "newMessage",
                CREATE_FORMATTED_MESSAGE_TYPE.getDescriptor(), false);
        mv.areturn(MESSAGE_TYPE);
    }

    private static String getCacheClassName(final String internalClassName) {
        return StringUtils.substringBefore(internalClassName, '$') + Constants.LOCATION_CACHE_SUFFIX;
    }

    public static Path getCacheClassFile(final Path classFile) {
        final String fileName = classFile.getFileName().toString();
        final String cacheFileName = LocationCacheGenerator.getCacheClassName(StringUtils.removeEnd(fileName, ".class"))
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

    /**
     * Describes the methods and fields of a specific location cache class.
     *
     */
    private static class LocationCacheContents {
        private final List<StackTraceElement> locations = new CopyOnWriteArrayList<>();
        private Set<SupplierLambdaType> lambdas = EnumSet.noneOf(SupplierLambdaType.class);

        public int addLocation(final String internalClassName, final String methodName, final String fileName,
                final int lineNumber) {
            final StackTraceElement location = new StackTraceElement(internalClassName.replaceAll("/", "."), methodName,
                    fileName, lineNumber);
            locations.add(location);
            return locations.indexOf(location);
        }

        public List<StackTraceElement> getLocations() {
            return locations;
        }

        public boolean addLambda(SupplierLambdaType type) {
            return lambdas.add(type);
        }

        public Set<SupplierLambdaType> getLambdas() {
            return lambdas;
        }
    }
}
