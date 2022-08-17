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
package org.apache.logging.log4j.jackson;

import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Used to set up {@link SimpleModule} from different {@link SimpleModule} subclasses.
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 */
public class SimpleModuleInitializer {
    public void initialize(final SimpleModule simpleModule, final boolean objectMessageAsJsonObject) {
        // Workaround because mix-ins do not work for classes that already have a built-in deserializer.
        // See Jackson issue 429.
        simpleModule.addDeserializer(StackTraceElement.class, new Log4jStackTraceElementDeserializer());
        simpleModule.addDeserializer(ContextStack.class, new MutableThreadContextStackDeserializer());
        if (objectMessageAsJsonObject) {
            simpleModule.addSerializer(ObjectMessage.class, new ObjectMessageSerializer());
        }
        simpleModule.addSerializer(Message.class, new MessageSerializer());
    }
}
