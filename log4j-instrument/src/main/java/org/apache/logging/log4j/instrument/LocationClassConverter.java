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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;

import org.apache.logging.log4j.instrument.log4j2.LoggerConversionHandler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class LocationClassConverter implements ClassFileTransformer {

    /**
     * Adds location information to a classfile.
     *
     * @param src           original classfile
     * @param dest          transformed classfile
     * @param locationCache a container for location data
     */
    public void convert(InputStream src, OutputStream dest, LocationCache locationCache) throws IOException {
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final LocationClassVisitor converter = new LocationClassVisitor(writer, locationCache);
        converter.addClassConversionHandler(new LoggerConversionHandler());
        new ClassReader(src).accept(converter, ClassReader.EXPAND_FRAMES);

        dest.write(writer.toByteArray());
    }
}
