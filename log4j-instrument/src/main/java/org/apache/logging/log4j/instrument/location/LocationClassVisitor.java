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

import org.apache.logging.log4j.instrument.location.LocationCache.LocationCacheValue;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class LocationClassVisitor extends ClassVisitor {

    private final LocationCache locationCache;

    private String fileName;
    private String declaringClass;
    private String methodName;

    protected LocationClassVisitor(ClassVisitor cv, LocationCache locationCache) {
        super(Opcodes.ASM9, cv);
        this.locationCache = locationCache;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.declaringClass = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        this.fileName = source;
        super.visitSource(source, debug);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        this.methodName = name;
        final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return mv != null ? new LocationMethodVisitor(this, mv, access, name, descriptor) : null;
    }

    public LocationCacheValue addStackTraceElement(final int lineNumber) {
        return locationCache.addLocation(declaringClass, methodName, fileName, lineNumber);
    }

}
