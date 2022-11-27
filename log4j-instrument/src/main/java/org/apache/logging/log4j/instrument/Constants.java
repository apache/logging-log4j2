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

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class Constants {

    public static final Type[] EMPTY_ARRAY = new Type[0];
    public static final String LOCATION_CACHE_SUFFIX = "$$Log4j2$$Cache";

    // JDK types
    public static final Type LAMBDA_METAFACTORY_TYPE = Type.getType(LambdaMetafactory.class);
    public static final Type LOOKUP_TYPE = Type.getType(MethodHandles.Lookup.class);
    public static final Type METHOD_HANDLE_TYPE = Type.getType(MethodHandle.class);
    public static final Type METHOD_TYPE_TYPE = Type.getType(MethodType.class);
    public static final Type OBJECT_TYPE = Type.getType(Object.class);
    public static final Type STACK_TRACE_ELEMENT_TYPE = Type.getType(StackTraceElement.class);
    public static final Type STACK_TRACE_ELEMENT_ARRAY_TYPE = Type.getType(StackTraceElement[].class);
    public static final Type STRING_TYPE = Type.getType(String.class);
    public static final Type THROWABLE_TYPE = Type.getType(Throwable.class);


    // Log4j2 types
    public static final Type LOG_BUILDER_TYPE = Type.getObjectType("org/apache/logging/log4j/LogBuilder");
    public static final Type LOGGER_TYPE = Type.getObjectType("org/apache/logging/log4j/Logger");
    public static final Type LEVEL_TYPE = Type.getObjectType("org/apache/logging/log4j/Level");
    public static final Type MARKER_TYPE = Type.getObjectType("org/apache/logging/log4j/Marker");
    public static final Type MESSAGE_TYPE = Type.getObjectType("org/apache/logging/log4j/message/Message");
    public static final Type MESSAGE_SUPPLIER_TYPE = Type.getObjectType("org/apache/logging/log4j/util/MessageSupplier");
    public static final Type SUPPLIER_TYPE = Type.getObjectType("org/apache/logging/log4j/util/Supplier");
    public static final Type SUPPLIER_ARRAY_TYPE = Type.getType("[" + SUPPLIER_TYPE.getDescriptor());

    // LogBuilder methods types
    private static final String NO_ARGS_DESC = Type.getMethodDescriptor(LOG_BUILDER_TYPE);
    public static final Method AT_DEBUG_METHOD = new Method("atDebug", NO_ARGS_DESC);
    public static final Method AT_ERROR_METHOD = new Method("atError", NO_ARGS_DESC);
    public static final Method AT_FATAL_METHOD = new Method("atFatal", NO_ARGS_DESC);
    public static final Method AT_INFO_METHOD = new Method("atInfo", NO_ARGS_DESC);
    public static final Method AT_TRACE_METHOD = new Method("atTrace", NO_ARGS_DESC);
    public static final Method AT_WARN_METHOD = new Method("atWarn", NO_ARGS_DESC);
    public static final Method AT_LEVEL_METHOD = new Method("atLevel", Type.getMethodDescriptor(LOG_BUILDER_TYPE, LEVEL_TYPE));
    public static final Method WITH_LOCATION_METHOD = new Method("withLocation",
            Type.getMethodDescriptor(LOG_BUILDER_TYPE, STACK_TRACE_ELEMENT_TYPE));
    public static final Method WITH_MARKER_METHOD = new Method("withMarker",
            Type.getMethodDescriptor(LOG_BUILDER_TYPE, MARKER_TYPE));
    public static final Method WITH_THROWABLE_METHOD = new Method("withThrowable",
            Type.getMethodDescriptor(LOG_BUILDER_TYPE, THROWABLE_TYPE));

    // Handles
    private static String OBJECT_SUPPLIER_DESC = Type.getMethodDescriptor(OBJECT_TYPE);
    public static Type OBJECT_FACTORY_TYPE = Type.getType(OBJECT_SUPPLIER_DESC);
    private static String MESSAGE_SUPPLIER_DESC = Type.getMethodDescriptor(MESSAGE_TYPE);
    public static Type MESSAGE_FACTORY_TYPE = Type.getType(MESSAGE_SUPPLIER_DESC);
    public static final Method SUPPLIER_GET = new Method("get", OBJECT_SUPPLIER_DESC);
    public static final Handle JAVA_SUPPLIER_GET_HANDLE = new Handle(Opcodes.H_INVOKEINTERFACE,
            Type.getType(Supplier.class).getInternalName(), "get", OBJECT_SUPPLIER_DESC, true);
    public static final Method MESSAGE_SUPPLIER_GET = new Method("get", MESSAGE_SUPPLIER_DESC);
    public static final Handle MESSAGE_SUPPLIER_GET_HANDLE = new Handle(Opcodes.H_INVOKEINTERFACE,
            MESSAGE_SUPPLIER_TYPE.getInternalName(), "get", MESSAGE_SUPPLIER_DESC, true);
    // Lambda that converts a MessageSupplier to a Supplier
    public static String MESSAGE_SUPPLIER_TO_SUPPLIER_DESC = Type.getMethodDescriptor(SUPPLIER_TYPE, MESSAGE_SUPPLIER_TYPE);

    private static final String LAMBDA_METAFACTORY_DESC = Type.getMethodDescriptor(Type.getType(CallSite.class),
            LOOKUP_TYPE, STRING_TYPE, METHOD_TYPE_TYPE, METHOD_TYPE_TYPE, METHOD_HANDLE_TYPE, METHOD_TYPE_TYPE);
    public static final Handle LAMBDA_METAFACTORY_HANDLE = new Handle(Opcodes.H_INVOKESTATIC,
            LAMBDA_METAFACTORY_TYPE.getInternalName(), "metafactory", LAMBDA_METAFACTORY_DESC, false);

    private Constants()
    {
        // prevent instantiation
    }
}
