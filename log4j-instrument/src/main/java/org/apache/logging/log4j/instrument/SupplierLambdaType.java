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

import org.objectweb.asm.Type;

import static org.apache.logging.log4j.instrument.Constants.ENTRY_MESSAGE_TYPE;
import static org.apache.logging.log4j.instrument.Constants.LOGGER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.MESSAGE_SUPPLIER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.MESSAGE_TYPE;
import static org.apache.logging.log4j.instrument.Constants.OBJECT_ARRAY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.OBJECT_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STRING_TYPE;
import static org.apache.logging.log4j.instrument.Constants.SUPPLIER_ARRAY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.SUPPLIER_TYPE;

/**
 * An enumeration of {@code org.apache.logging.log4j.util.Supplier<Message>} lambdas, named after the type of message and parameter types.
 *
 */
public enum SupplierLambdaType {
    MESSAGE_SUPPLIER(MESSAGE_SUPPLIER_TYPE),
    FORMATTED_MESSAGE(STRING_TYPE, OBJECT_ARRAY_TYPE),
    ENTRY_MESSAGE_MESSAGE(LOGGER_TYPE, MESSAGE_TYPE),
    ENTRY_MESSAGE_STRING_OBJECTS(LOGGER_TYPE, STRING_TYPE, OBJECT_ARRAY_TYPE),
    EXIT_MESSAGE_ENTRY_MESSAGE(LOGGER_TYPE, ENTRY_MESSAGE_TYPE),
    EXIT_MESSAGE_MESSAGE(LOGGER_TYPE, MESSAGE_TYPE),
    EXIT_MESSAGE_OBJECT_ENTRY_MESSAGE(LOGGER_TYPE, OBJECT_TYPE, ENTRY_MESSAGE_TYPE),
    EXIT_MESSAGE_OBJECT_MESSAGE(LOGGER_TYPE, OBJECT_TYPE, MESSAGE_TYPE),
    EXIT_MESSAGE_STRING_OBJECT(LOGGER_TYPE, STRING_TYPE, OBJECT_TYPE),
    ENTRY_MESSAGE_STRING_SUPPLIERS(LOGGER_TYPE, STRING_TYPE, SUPPLIER_ARRAY_TYPE);

    private final Type[] argumentTypes;

    private SupplierLambdaType(final Type... argumentTypes) {
        this.argumentTypes = argumentTypes;
    }

    /**
     * Returns the descriptor of the invokedynamic call.
     */
    public String getInvokedMethodDescriptor() {
        return Type.getMethodDescriptor(SUPPLIER_TYPE, argumentTypes);
    }

    /**
     * Returns the descriptor of the implementation method.
     */
    public String getImplementationMethodDescriptor() {
        return Type.getMethodDescriptor(MESSAGE_TYPE, argumentTypes);
    }

    public Type[] getArgumentTypes() {
        return argumentTypes;
    }
}
