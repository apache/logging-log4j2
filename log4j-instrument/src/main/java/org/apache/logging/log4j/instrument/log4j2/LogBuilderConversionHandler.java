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

import org.apache.logging.log4j.instrument.ClassConversionHandler;
import org.apache.logging.log4j.instrument.LocationMethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import static org.apache.logging.log4j.instrument.Constants.LOG_BUILDER_TYPE;

public class LogBuilderConversionHandler implements ClassConversionHandler {

    @Override
    public String getOwner() {
        return LOG_BUILDER_TYPE.getInternalName();
    }

    @Override
    public void handleMethodInstruction(LocationMethodVisitor mv, String name, String descriptor) {
        if ("withLocation".equals(name) && Type.getMethodDescriptor(LOG_BUILDER_TYPE).equals(descriptor)) {
            return;
        }
        mv.invokeInterface(LOG_BUILDER_TYPE, new Method(name, descriptor));
    }

}
