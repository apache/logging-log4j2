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
package org.apache.logging.log4j.instrument.log4j2;

import java.util.Arrays;

import org.apache.logging.log4j.instrument.ClassConversionHandler;
import org.apache.logging.log4j.instrument.Constants;
import org.apache.logging.log4j.instrument.ConversionException;
import org.apache.logging.log4j.instrument.LocationMethodVisitor;
import org.apache.logging.log4j.instrument.SupplierLambdaType;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import static org.apache.logging.log4j.instrument.Constants.AT_DEBUG_METHOD;
import static org.apache.logging.log4j.instrument.Constants.AT_ERROR_METHOD;
import static org.apache.logging.log4j.instrument.Constants.AT_FATAL_METHOD;
import static org.apache.logging.log4j.instrument.Constants.AT_INFO_METHOD;
import static org.apache.logging.log4j.instrument.Constants.AT_LEVEL_METHOD;
import static org.apache.logging.log4j.instrument.Constants.AT_TRACE_METHOD;
import static org.apache.logging.log4j.instrument.Constants.AT_WARN_METHOD;
import static org.apache.logging.log4j.instrument.Constants.ENTRY_MESSAGE_TYPE;
import static org.apache.logging.log4j.instrument.Constants.LEVEL_TYPE;
import static org.apache.logging.log4j.instrument.Constants.LOGGER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.LOG_AND_GET_METHOD;
import static org.apache.logging.log4j.instrument.Constants.LOG_BUILDER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.MARKER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.MESSAGE_SUPPLIER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.MESSAGE_TYPE;
import static org.apache.logging.log4j.instrument.Constants.OBJECT_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STRING_TYPE;
import static org.apache.logging.log4j.instrument.Constants.SUPPLIER_ARRAY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.SUPPLIER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.THROWABLE_TYPE;
import static org.apache.logging.log4j.instrument.Constants.WITH_MARKER_METHOD;
import static org.apache.logging.log4j.instrument.Constants.WITH_THROWABLE_METHOD;

public class LoggerConversionHandler implements ClassConversionHandler {

    private static final String CATCHING = "Catching";
    private static final String CATCHING_MARKER = "CATCHING_MARKER";
    private static final String ENTRY_MARKER = "ENTRY_MARKER";
    private static final String EXIT_MARKER = "EXIT_MARKER";
    private static final String THROWING = "Throwing";
    private static final String THROWING_MARKER = "THROWING_MARKER";
    // Argument list of `LogBuilder.log(String, Supplier...)`
    private static final Type[] LOG_BUILDER_LOG_STRING_SUPPLIER = new Type[] { STRING_TYPE, SUPPLIER_ARRAY_TYPE };
    // Argument list of `LogBuilder.log(Supplier<Message>)`
    private static final Type[] LOG_BUILDER_LOG_SUPPLIER_MESSAGE = new Type[] { SUPPLIER_TYPE };
    private static final Method LOG_BUILDER_LOG_SUPPLIER_METHOD = new Method("log",
            Type.getMethodDescriptor(Type.VOID_TYPE, LOG_BUILDER_LOG_SUPPLIER_MESSAGE));
    private static final Method LOG_BUILDER_LOG_STRING_METHOD = new Method("log",
            Type.getMethodDescriptor(Type.VOID_TYPE, STRING_TYPE));
    private static final Type ABSTRACT_LOGGER_TYPE = Type.getObjectType("org/apache/logging/log4j/spi/AbstractLogger");
    private static final Type[] MESSAGE_OBJECT_ARRAY = { MESSAGE_TYPE, OBJECT_TYPE };

    @Override
    public String getOwner() {
        return Constants.LOGGER_TYPE.getInternalName();
    }

    private void createLogBuilder(LocationMethodVisitor mv, String methodName) {
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
        mv.invokeInterface(LOGGER_TYPE, method);
    }

    @Override
    public void handleMethodInstruction(LocationMethodVisitor mv, String name, String descriptor) {
        switch (name) {
            case "debug":
            case "error":
            case "fatal":
            case "info":
            case "log":
            case "trace":
            case "warn":
                handleLogMethods(mv, name, descriptor);
                break;
            case "printf":
                handlePrintfMethods(mv, descriptor);
                break;
            case "always":
            case "atDebug":
            case "atError":
            case "atFatal":
            case "atInfo":
            case "atLevel":
            case "atTrace":
            case "atWarn":
                mv.invokeInterface(LOGGER_TYPE, new Method(name, descriptor));
                mv.storeLocation();
                break;
            case "catching":
            case "throwing":
                handleCatchingThrowing(mv, descriptor, "throwing".equals(name));
                break;
            case "isDebugEnabled":
            case "isEnabled":
            case "isErrorEnabled":
            case "isFatalEnabled":
            case "isInfoEnabled":
            case "isTraceEnabled":
            case "isWarnEnabled":
            case "logMessage":
                // These are NOPs
                mv.invokeInterface(LOGGER_TYPE, new Method(name, descriptor));
                break;
            case "traceEntry":
                handleTraceEntry(mv, descriptor);
                break;
            case "traceExit":
                handleTraceExit(mv, descriptor);
                break;
            default:
                throw new ConversionException("Unsupported method 'org.apache.logging.log4j.Logger#" + name + "'.");
        }
    }

    /**
     * Rewrites the most common methods: {@code log} and its level specializations.
     */
    private void handleLogMethods(LocationMethodVisitor mv, String name, String descriptor) {
        final Type[] types = Type.getArgumentTypes(descriptor);
        final int[] varIndexes = new int[types.length];
        int from = types.length > 0 && types[0].equals(LEVEL_TYPE) ? 1 : 0;
        int to = types.length;
        // Store arguments to local variables
        // TODO: most of the time there is a more efficient way
        for (int i = to - 1; i >= from; i--) {
            varIndexes[i] = mv.nextLocal();
            mv.storeLocal(varIndexes[i], types[i]);
        }
        // create the LogBuilder
        createLogBuilder(mv, name);
        mv.storeLocation();
        // Marker argument
        if (from < to && types[from].equals(MARKER_TYPE)) {
            mv.loadLocal(varIndexes[from], MARKER_TYPE);
            mv.invokeInterface(LOG_BUILDER_TYPE, WITH_MARKER_METHOD);
            from++;
        }
        // Throwable argument
        if (from < to && types[to - 1].equals(THROWABLE_TYPE)) {
            mv.loadLocal(varIndexes[to - 1], THROWABLE_TYPE);
            mv.invokeInterface(LOG_BUILDER_TYPE, WITH_THROWABLE_METHOD);
            to--;
        }
        // Call log(...)
        final Type[] arguments;
        // We need to replace (Supplier<?>) with ("{}", Supplier<?>)
        if (SUPPLIER_TYPE.equals(types[from])) {
            mv.push("{}");
            mv.push(1);
            mv.newArray(SUPPLIER_TYPE);
            mv.dup();
            mv.push(0);
            mv.loadLocal(varIndexes[from], types[from]);
            mv.arrayStore(SUPPLIER_TYPE);
            arguments = LOG_BUILDER_LOG_STRING_SUPPLIER;
            // We need to convert MessageSupplier to Supplier<Message>
        } else if (MESSAGE_SUPPLIER_TYPE.equals(types[from])) {
            mv.loadLocal(varIndexes[from], types[from]);
            mv.invokeSupplierLambda(SupplierLambdaType.MESSAGE_SUPPLIER);
            arguments = LOG_BUILDER_LOG_SUPPLIER_MESSAGE;
        } else {
            for (int i = from; i < to; i++) {
                mv.loadLocal(varIndexes[i], types[i]);
            }
            arguments = Arrays.copyOfRange(types, from, to);
        }
        final Method logMethod = new Method("log", Type.VOID_TYPE, arguments);
        mv.invokeInterface(LOG_BUILDER_TYPE, logMethod);
    }

    private void handlePrintfMethods(LocationMethodVisitor mv, String descriptor) {
        final Type[] types = Type.getArgumentTypes(descriptor);
        // Transform the last two arguments into a supplier
        mv.invokeSupplierLambda(SupplierLambdaType.FORMATTED_MESSAGE);
        int supplierIndex = mv.nextLocal();
        mv.storeLocal(supplierIndex, SUPPLIER_TYPE);
        int markerIndex = -1;
        if (types[1].equals(MARKER_TYPE)) {
            markerIndex = mv.nextLocal();
            mv.storeLocal(markerIndex, MARKER_TYPE);
        }
        mv.invokeInterface(LOGGER_TYPE, AT_LEVEL_METHOD);
        mv.storeLocation();
        if (markerIndex >= 0) {
            mv.loadLocal(markerIndex, MARKER_TYPE);
            mv.invokeInterface(LOG_BUILDER_TYPE, WITH_MARKER_METHOD);
        }
        mv.loadLocal(supplierIndex, SUPPLIER_TYPE);
        mv.invokeInterface(LOG_BUILDER_TYPE, LOG_BUILDER_LOG_SUPPLIER_METHOD);
    }

    private void handleCatchingThrowing(LocationMethodVisitor mv, String descriptor, boolean throwing) {
        final boolean hasLevel = Type.getArgumentTypes(descriptor).length > 1;
        final int throwableIndex = mv.nextLocal();
        mv.storeLocal(throwableIndex, THROWABLE_TYPE);
        if (hasLevel) {
            mv.invokeInterface(LOGGER_TYPE, AT_LEVEL_METHOD);
        } else {
            mv.invokeInterface(LOGGER_TYPE, AT_ERROR_METHOD);
        }
        mv.storeLocation();
        mv.loadLocal(throwableIndex, THROWABLE_TYPE);
        mv.invokeInterface(LOG_BUILDER_TYPE, WITH_THROWABLE_METHOD);
        mv.getStatic(ABSTRACT_LOGGER_TYPE, throwing ? THROWING_MARKER : CATCHING_MARKER, MARKER_TYPE);
        mv.invokeInterface(LOG_BUILDER_TYPE, WITH_MARKER_METHOD);
        mv.push(throwing ? THROWING : CATCHING);
        mv.invokeInterface(LOG_BUILDER_TYPE, LOG_BUILDER_LOG_STRING_METHOD);
        if (throwing) {
            mv.loadLocal(throwableIndex, THROWABLE_TYPE);
        }
    }

    private void handleTraceEntry(LocationMethodVisitor mv, String descriptor) {
        final Type[] types = Type.getArgumentTypes(descriptor);
        final int[] vars = new int[types.length];
        for (int i = vars.length - 1; i >= 0; i--) {
            vars[i] = mv.nextLocal();
            mv.storeLocal(vars[i]);
        }
        // only Logger on stack
        mv.dup();
        final int loggerIdx = mv.nextLocal();
        mv.storeLocal(loggerIdx, LOGGER_TYPE);
        mv.invokeInterface(LOGGER_TYPE, AT_TRACE_METHOD);
        mv.storeLocation();
        mv.getStatic(ABSTRACT_LOGGER_TYPE, ENTRY_MARKER, MARKER_TYPE);
        mv.invokeInterface(LOG_BUILDER_TYPE, WITH_MARKER_METHOD);
        mv.loadLocal(loggerIdx, LOGGER_TYPE);
        if (types.length == 0) {
            mv.push((String) null);
            mv.push((String) null);
            mv.invokeSupplierLambda(SupplierLambdaType.ENTRY_MESSAGE_STRING_OBJECTS);
        } else if (types[0].equals(MESSAGE_TYPE)) {
            mv.loadLocal(vars[0]);
            mv.invokeSupplierLambda(SupplierLambdaType.ENTRY_MESSAGE_MESSAGE);
        } else {
            if (types.length == 1) {
                mv.push((String) null);
            }
            for (int i = 0; i < vars.length; i++) {
                mv.loadLocal(vars[i]);
            }
            final boolean usesSuppliers = types[types.length - 1].equals(SUPPLIER_ARRAY_TYPE);
            mv.invokeSupplierLambda(usesSuppliers ? SupplierLambdaType.ENTRY_MESSAGE_STRING_SUPPLIERS
                    : SupplierLambdaType.ENTRY_MESSAGE_STRING_OBJECTS);
        }
        mv.invokeInterface(LOG_BUILDER_TYPE, LOG_AND_GET_METHOD);
    }

    private void handleTraceExit(LocationMethodVisitor mv, String descriptor) {
        final Type[] types = Type.getArgumentTypes(descriptor);
        final int[] vars = new int[types.length];
        for (int i = vars.length - 1; i >= 0; i--) {
            vars[i] = mv.nextLocal();
            mv.storeLocal(vars[i]);
        }
        // only Logger on stack
        mv.dup();
        final int loggerIdx = mv.nextLocal();
        mv.storeLocal(loggerIdx, LOGGER_TYPE);
        mv.invokeInterface(LOGGER_TYPE, AT_TRACE_METHOD);
        mv.storeLocation();
        mv.getStatic(ABSTRACT_LOGGER_TYPE, EXIT_MARKER, MARKER_TYPE);
        mv.invokeInterface(LOG_BUILDER_TYPE, WITH_MARKER_METHOD);
        mv.loadLocal(loggerIdx, LOGGER_TYPE);
        if (types.length == 0) {
            mv.push((String) null);
            mv.push((String) null);
            mv.invokeSupplierLambda(SupplierLambdaType.EXIT_MESSAGE_STRING_OBJECT);
        } else if (Arrays.deepEquals(types, MESSAGE_OBJECT_ARRAY)) {
            // Invert arguments
            mv.loadLocal(vars[1]);
            mv.loadLocal(vars[0]);
            mv.invokeSupplierLambda(SupplierLambdaType.EXIT_MESSAGE_OBJECT_MESSAGE);
        } else if (ENTRY_MESSAGE_TYPE.equals(types[0])) {
            final boolean hasResult = types.length == 2;
            if (hasResult) {
                mv.loadLocal(vars[1]);
            }
            mv.loadLocal(vars[0]);
            mv.invokeSupplierLambda(hasResult ? SupplierLambdaType.EXIT_MESSAGE_OBJECT_ENTRY_MESSAGE
                    : SupplierLambdaType.EXIT_MESSAGE_ENTRY_MESSAGE);
        } else {
            final boolean hasFormat = STRING_TYPE.equals(types[0]);
            if (hasFormat) {
                mv.loadLocal(vars[0]);
            } else {
                mv.push((String) null);
            }
            mv.loadLocal(vars[hasFormat ? 1 : 0], OBJECT_TYPE);
            mv.invokeSupplierLambda(SupplierLambdaType.EXIT_MESSAGE_STRING_OBJECT);
        }
        mv.invokeInterface(LOG_BUILDER_TYPE, LOG_BUILDER_LOG_SUPPLIER_METHOD);
        // except void methods traceExit() and traceExit(EntryMessage)
        if (types.length != 0 && (types.length > 1 || !ENTRY_MESSAGE_TYPE.equals(types[0]))) {
            mv.loadLocal(vars[vars.length - 1]);
        }
    }
}
