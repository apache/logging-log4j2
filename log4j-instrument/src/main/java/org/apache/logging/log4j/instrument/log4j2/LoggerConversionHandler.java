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
import org.objectweb.asm.Type;
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
import static org.apache.logging.log4j.instrument.Constants.STRING_TYPE;
import static org.apache.logging.log4j.instrument.Constants.SUPPLIER_ARRAY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.SUPPLIER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.THROWABLE_TYPE;
import static org.apache.logging.log4j.instrument.Constants.WITH_MARKER_METHOD;
import static org.apache.logging.log4j.instrument.Constants.WITH_THROWABLE_METHOD;

public class LoggerConversionHandler implements ClassConversionHandler {

    // Argument list of `LogBuilder.log(String, Supplier...)`
    private static Type[] LOGBUILDER_LOG_STRING_SUPPLIER = new Type[] { STRING_TYPE, SUPPLIER_ARRAY_TYPE };
    // Argument list of `LogBuilder.log(Supplier<Message>)`
    private static Type[] LOGBUILDER_LOG_SUPPLIER_MESSAGE = new Type[] { SUPPLIER_TYPE };

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
                    arguments = LOGBUILDER_LOG_STRING_SUPPLIER;
                    // We need to convert MessageSupplier to Supplier<Message>
                } else if (MESSAGE_SUPPLIER_TYPE.equals(types[from])) {
                    mv.loadLocal(varIndexes[from], types[from]);
                    mv.invokeDynamic("get",
                            MESSAGE_SUPPLIER_TO_SUPPLIER_DESC,
                            LAMBDA_METAFACTORY_HANDLE,
                            OBJECT_FACTORY_TYPE,
                            JAVA_SUPPLIER_GET_HANDLE,
                            MESSAGE_FACTORY_TYPE);
                    arguments = LOGBUILDER_LOG_SUPPLIER_MESSAGE;
                } else {
                    for (int i = from; i < to; i++) {
                        mv.loadLocal(varIndexes[i], types[i]);
                    }
                    arguments = Arrays.copyOfRange(types, from, to);
                }
                final Method logMethod = new Method("log", Type.VOID_TYPE, arguments);
                mv.invokeInterface(LOG_BUILDER_TYPE, logMethod);
                break;
            default:
        }
    }

}
