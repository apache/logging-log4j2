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

import org.apache.logging.log4j.migration.ConversionException;
import org.apache.logging.log4j.migration.ConverterProfile;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Checks the original code for features that this weaver does not support.
 *
 */
public class JclClassValidator extends ClassVisitor {

    private final ConverterProfile profile;

    public JclClassValidator(ClassVisitor classVisitor, ConverterProfile profile) {
        super(Opcodes.ASM9, classVisitor);
        this.profile = profile;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (JclTypes.LOGFACTORY.equals(superName)) {
            throw new ConversionException("Classes extending 'LogFactory' are not supported.");
        }
        for (final String iface : interfaces) {
            if (JclTypes.LOG.equals(iface)) {
                throw new ConversionException("Classes implementing 'Log' are not supported.");
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        return new JclMethodValidator(super.visitMethod(access, name, descriptor, signature, exceptions));
    }

    private class JclMethodValidator extends MethodVisitor {

        protected JclMethodValidator(MethodVisitor methodVisitor) {
            super(Opcodes.ASM9, methodVisitor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (JclTypes.LOGFACTORY.equals(owner)) {
                switch (name) {
                    case JclTypes.GET_FACTORY_NAME:
                    case JclTypes.GET_LOG_NAME:
                    case JclTypes.GET_INSTANCE_NAME:
                        break;
                    default:
                        if (profile == ConverterProfile.FULL) {
                            throw new ConversionException(
                                    "Usage of the 'LogFactory#" + name + "' method is not supported.");
                        }
                }
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

    }
}
