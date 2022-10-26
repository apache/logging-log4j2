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
package org.apache.logging.log4j.migration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public abstract class AbstractClassConverter implements ClassFileTransformer, Converter {

    protected final ConverterProfile profile;

    protected abstract ClassVisitor createRemapper(final ClassVisitor classVisitor);

    protected abstract ClassVisitor createValidator(final ClassVisitor classVisitor);

    public AbstractClassConverter(ConverterProfile profile) {
        this.profile = profile;
    }

    @Override
    public boolean accepts(String filename) {
        return filename.endsWith(".class");
    }

    @Override
    public void convert(String path, InputStream src, OutputStream dest) throws IOException {
        final ClassReader reader = new ClassReader(src);
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        final ClassVisitor remapper = createRemapper(writer);
        final ClassVisitor validator = createValidator(remapper);
        reader.accept(validator, 0);
        dest.write(writer.toByteArray());
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(classfileBuffer);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    convertInternal(className, inputStream, outputStream, profile);
                } catch (IOException e) {
                    throw new IllegalClassFormatException(e.getLocalizedMessage());
                }
                return outputStream.toByteArray();
            }

    protected void convertInternal(final String className, final InputStream src, final OutputStream dest, final ConverterProfile profile) throws IOException {
        final ClassReader reader = new ClassReader(src);
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        final ClassVisitor remapper = createRemapper(writer);
        final ClassVisitor validator = createValidator(remapper);
        reader.accept(validator, 0);
        dest.write(writer.toByteArray());
    }

}
