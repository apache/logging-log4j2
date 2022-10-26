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
package org.apache.logging.log4j.migration.jcl;

import org.apache.logging.log4j.migration.Log4j2Types;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;

public class JclClassRemapper extends ClassRemapper {

    public JclClassRemapper(ClassVisitor classVisitor, Remapper remapper) {
        super(Opcodes.ASM9, classVisitor, remapper);
    }

    @Override
    protected MethodVisitor createMethodRemapper(MethodVisitor methodVisitor) {
        return new JclMethodRemapper(methodVisitor, remapper);
    }

    private static class JclMethodRemapper extends MethodRemapper {

        public JclMethodRemapper(MethodVisitor methodVisitor, Remapper remapper) {
            super(Opcodes.ASM9, methodVisitor, remapper);
        }

        @Override
        public void visitMethodInsn(int opcodeAndSource, String owner, String name, String descriptor,
                boolean isInterface) {
            if (JclTypes.LOGFACTORY.equals(owner)) {
                switch (name) {
                    // Call LogManager.getContext(false) (method descriptor changes)
                    case JclTypes.GET_FACTORY_NAME:
                        mv.visitInsn(Opcodes.ICONST_0);
                        mv.visitMethodInsn(opcodeAndSource, Log4j2Types.LOGMANAGER, Log4j2Types.GET_CONTEXT_NAME,
                                Log4j2Types.GET_CONTEXT_BOOLEAN_DESC, isInterface);
                        break;
                    case JclTypes.GET_LOG_NAME:
                        final String logManagerDescriptor = JclTypes.GET_LOG_STRING_DESC.equals(descriptor)
                                ? Log4j2Types.LOGMANAGER_GET_LOGGER_STRING_DESC
                                : Log4j2Types.LOGMANAGER_GET_LOGGER_CLASS_DESC;
                        mv.visitMethodInsn(opcodeAndSource, Log4j2Types.LOGMANAGER, Log4j2Types.GET_LOGGER_NAME,
                                logManagerDescriptor, isInterface);
                        break;
                    case JclTypes.GET_INSTANCE_NAME:
                        final String loggerContextDescriptor = JclTypes.GET_INSTANCE_STRING_DESC.equals(descriptor)
                                ? Log4j2Types.LOGGERCONTEXT_GET_LOGGER_STRING_DESC
                                : Log4j2Types.LOGGERCONTEXT_GET_LOGGER_CLASS_DESC;
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Log4j2Types.LOGGERCONTEXT, Log4j2Types.GET_LOGGER_NAME,
                                loggerContextDescriptor, true);
                        break;
                    default:
                        // Ignore the call
                        final int argSize = Type.getArgumentsAndReturnSizes(descriptor) >> 2;
                        for (int i = 0; i < argSize; i++) {
                            mv.visitInsn(Opcodes.POP);
                        }
                }
            } else {
                super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
            }
            ;
        }

    }
}
