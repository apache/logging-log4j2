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

import java.util.Map;

import org.apache.logging.log4j.instrument.LocationCache.LocationCacheValue;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

import static org.apache.logging.log4j.instrument.Constants.LOG_BUILDER_TYPE;
import static org.apache.logging.log4j.instrument.Constants.OBJECT_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STACK_TRACE_ELEMENT_ARRAY_TYPE;
import static org.apache.logging.log4j.instrument.Constants.STACK_TRACE_ELEMENT_TYPE;
import static org.apache.logging.log4j.instrument.Constants.WITH_LOCATION_METHOD;

public class LocationMethodVisitor extends GeneratorAdapter {

    private final LocationClassVisitor locationClassVisitor;
    private final Map<String, ClassConversionHandler> handlers;

    // A pool of local variables
    private final Integer[] localVariables = new Integer[12];
    private final Label[] startLabels = new Label[12];
    // Next available variable index
    private int nextVariable = 0;

    private int lineNumber;
    private Label currentLabel;

    protected LocationMethodVisitor(final LocationClassVisitor locationClassVisitor,
            final Map<String, ClassConversionHandler> handlers, final MethodVisitor mv, final int access,
            final String name, final String descriptor) {
        super(Opcodes.ASM9, mv, access, name, descriptor);
        this.locationClassVisitor = locationClassVisitor;
        this.handlers = handlers;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.lineNumber = line;
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        resetLocals();
        final ClassConversionHandler handler = handlers.get(owner);
        if (handler != null) {
            handler.handleMethodInstruction(this, name, descriptor);
        } else {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }

    /**
     * Assuming the top of the stack holds a {@code LogBuilder}, add location
     * information to it.
     */
    public void storeLocation() {
        final LocationCacheValue location = locationClassVisitor.addStackTraceElement(lineNumber);
        getStatic(location.getType(), location.getFieldName(), STACK_TRACE_ELEMENT_ARRAY_TYPE);
        push(location.getIndex());
        arrayLoad(STACK_TRACE_ELEMENT_TYPE);
        invokeInterface(LOG_BUILDER_TYPE, WITH_LOCATION_METHOD);
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

    private void resetLocals() {
        nextVariable = 0;
    }

    public int nextLocal() {
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
