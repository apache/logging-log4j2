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

import java.util.Arrays;

import org.apache.logging.log4j.instrument.ConversionException;
import org.apache.logging.log4j.instrument.location.LocationCache.LocationCacheValue;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import static org.apache.logging.log4j.instrument.Constants.AT_DEBUG_METHOD;
import static org.apache.logging.log4j.instrument.Constants.AT_ERROR_METHOD;
import static org.apache.logging.log4j.instrument.Constants.AT_FATAL_METHOD;
import static org.apache.logging.log4j.instrument.Constants.AT_INFO_METHOD;
import static org.apache.logging.log4j.instrument.Constants.AT_LEVEL_METHOD;
import static org.apache.logging.log4j.instrument.Constants.AT_TRACE_METHOD;
import static org.apache.logging.log4j.instrument.Constants.AT_WARN_METHOD;
import static org.apache.logging.log4j.instrument.Constants.JAVA_SUPPLIER_GET_HANDLE;
import static org.apache.logging.log4j.instrument.Constants.LAMBDA_METAFACTORY_HANDLE;
import static org.apache.logging.log4j.instrument.Constants.LEVEL_TYPE;
import static org.apache.logging.log4j.instrument.Constants.LOGGER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.LOG_BUILDER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.MARKER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.MESSAGE_FACTORY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.MESSAGE_SUPPLIER_TO_SUPPLIER_DESC;
import static org.apache.logging.log4j.instrument.Constants.MESSAGE_SUPPLIER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.OBJECT_FACTORY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.OBJECT_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STACK_TRACE_ELEMENT_ARRAY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STACK_TRACE_ELEMENT_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STRING_TYPE;
import static org.apache.logging.log4j.instrument.Constants.SUPPLIER_ARRAY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.SUPPLIER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.THROWABLE_TYPE;
import static org.apache.logging.log4j.instrument.Constants.WITH_LOCATION_METHOD;
import static org.apache.logging.log4j.instrument.Constants.WITH_MARKER_METHOD;
import static org.apache.logging.log4j.instrument.Constants.WITH_THROWABLE_METHOD;

class LocationMethodVisitor extends GeneratorAdapter {

    // Argument list of `LogBuilder.log(String, Supplier...)`
    private static Type[] LOGBUILDER_LOG_STRING_SUPPLIER = new Type[] { STRING_TYPE, SUPPLIER_ARRAY_TYPE };
    // Argument list of `LogBuilder.log(Supplier<Message>)`
    private static Type[] LOGBUILDER_LOG_SUPPLIER_MESSAGE = new Type[] { SUPPLIER_TYPE };

    private final LocationClassVisitor locationClassVisitor;

    // A pool of local variables
    private final Integer[] localVariables = new Integer[12];
    private final Label[] startLabels = new Label[12];
    // Next available variable index
    private int nextVariable = 0;

    private int lineNumber;
    private Label currentLabel;

    protected LocationMethodVisitor(LocationClassVisitor locationClassVisitor, final MethodVisitor mv, final int access,
            final String name, final String descriptor) {
        super(Opcodes.ASM9, mv, access, name, descriptor);
        this.locationClassVisitor = locationClassVisitor;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.lineNumber = line;
        super.visitLineNumber(line, start);
    }

    private void createLogBuilder(final String methodName) {
        final Method method;
        switch (methodName) {
            case "debug":
                method = AT_DEBUG_METHOD;
                break;
            case "error":
                method = AT_ERROR_METHOD;
                break;
            case "fatal":
                method = AT_FATAL_METHOD;
                break;
            case "info":
                method = AT_INFO_METHOD;
                break;
            case "log":
                method = AT_LEVEL_METHOD;
                break;
            case "trace":
                method = AT_TRACE_METHOD;
                break;
            case "warn":
                method = AT_WARN_METHOD;
                break;
            default:
                throw new ConversionException("Unknown logging method " + methodName);
        }
        invokeInterface(LOGGER_TYPE, method);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (LOGGER_TYPE.getInternalName().equals(owner)) {
            switch (name) {
                case "debug":
                case "error":
                case "fatal":
                case "info":
                case "log":
                case "trace":
                case "warn":
                    final Type[] types = Type.getArgumentTypes(descriptor);
                    final int[] varIndexes = new int[types.length];
                    int from = types.length > 0 && types[0].equals(LEVEL_TYPE) ? 1 : 0;
                    int to = types.length;
                    // Store arguments to local variables
                    // TODO: most of the time there is a more efficient way
                    for (int i = to - 1; i >= from; i--) {
                        varIndexes[i] = nextVariable();
                        storeLocal(varIndexes[i], types[i]);
                    }
                    // create the LogBuilder
                    createLogBuilder(name);
                    final LocationCacheValue location = locationClassVisitor.addStackTraceElement(lineNumber);
                    getStatic(location.getType(), location.getFieldName(), STACK_TRACE_ELEMENT_ARRAY_TYPE);
                    push(location.getIndex());
                    arrayLoad(STACK_TRACE_ELEMENT_TYPE);
                    invokeInterface(LOG_BUILDER_TYPE, WITH_LOCATION_METHOD);
                    // Marker argument
                    if (from < to && types[from].equals(MARKER_TYPE)) {
                        loadLocal(varIndexes[from], MARKER_TYPE);
                        invokeInterface(LOG_BUILDER_TYPE, WITH_MARKER_METHOD);
                        from++;
                    }
                    // Throwable argument
                    if (from < to && types[to - 1].equals(THROWABLE_TYPE)) {
                        loadLocal(varIndexes[to - 1], THROWABLE_TYPE);
                        invokeInterface(LOG_BUILDER_TYPE, WITH_THROWABLE_METHOD);
                        to--;
                    }
                    // Call log(...)
                    final Type[] arguments;
                    // We need to replace (Supplier<?>) with ("{}", Supplier<?>)
                    if (SUPPLIER_TYPE.equals(types[from])) {
                        push("{}");
                        push(1);
                        newArray(SUPPLIER_TYPE);
                        dup();
                        push(0);
                        loadLocal(varIndexes[from], types[from]);
                        arrayStore(SUPPLIER_TYPE);
                        arguments = LOGBUILDER_LOG_STRING_SUPPLIER;
                    // We need to convert MessageSupplier to Supplier<Message>
                    } else if (MESSAGE_SUPPLIER_TYPE.equals(types[from])) {
                        loadLocal(varIndexes[from], types[from]);
                        invokeDynamic("get",
                                MESSAGE_SUPPLIER_TO_SUPPLIER_DESC,
                                LAMBDA_METAFACTORY_HANDLE,
                                OBJECT_FACTORY_TYPE,
                                JAVA_SUPPLIER_GET_HANDLE,
                                MESSAGE_FACTORY_TYPE);
                        arguments = LOGBUILDER_LOG_SUPPLIER_MESSAGE;
                    } else {
                        for (int i = from; i < to; i++) {
                            loadLocal(varIndexes[i], types[i]);
                        }
                        arguments = Arrays.copyOfRange(types, from, to);
                    }
                    resetVariables();
                    final Method logMethod = new Method("log", Type.VOID_TYPE, arguments);
                    invokeInterface(LOG_BUILDER_TYPE, logMethod);
                    return;
                default:
            }
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitLabel(Label label) {
        currentLabel = label;
        super.visitLabel(label);
    }

    @Override
    public void visitEnd() {
        for (int i = 0; i < startLabels.length; i++) {
            final Label label = startLabels[i];
            if (label != null) {
                // the generator adapter uses different variable indexes
                mv.visitLocalVariable("log4j2$$p" + i, OBJECT_TYPE.getDescriptor(), null, label, currentLabel,
                        localVariables[i]);
            }
        }
        super.visitEnd();
    }

    private void resetVariables() {
        nextVariable = 0;
    }

    private int nextVariable() {
        Integer varIndex = localVariables[nextVariable];
        if (varIndex == null) {
            varIndex = newLocal(OBJECT_TYPE);
            localVariables[nextVariable] = varIndex;
            // remember first usage of variable
            startLabels[nextVariable] = currentLabel;
        }
        nextVariable++;
        return varIndex;
    }
}
