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

import static org.apache.logging.log4j.instrument.Constants.MESSAGE_SUPPLIER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.OBJECT_ARRAY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STRING_TYPE;
import static org.apache.logging.log4j.instrument.Constants.SUPPLIER_TYPE;

/**
 * An enumeration of {@code org.apache.logging.log4j.util.Supplier<Message>} lambdas, named after the type of message and parameter types.
 *
 */
public enum SupplierLambdaType {
    MESSAGE_SUPPLIER(MESSAGE_SUPPLIER_TYPE),
    FORMATTED_MESSAGE(STRING_TYPE, OBJECT_ARRAY_TYPE);

    private final Type methodType;

    private SupplierLambdaType(final Type... arguments) {
        this.methodType = Type.getMethodType(SUPPLIER_TYPE, arguments);
    }

    public String getMethodDescriptor() {
        return methodType.getDescriptor();
    }
}
