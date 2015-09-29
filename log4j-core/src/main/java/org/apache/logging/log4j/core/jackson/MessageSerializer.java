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
package org.apache.logging.log4j.core.jackson;

import java.io.IOException;

import org.apache.logging.log4j.message.Message;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

/**
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 */
final class MessageSerializer extends StdScalarSerializer<Message> {

    private static final long serialVersionUID = 1L;

    MessageSerializer() {
        super(Message.class);
    }

    @Override
    public void serialize(final Message value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException,
            JsonGenerationException {
        jgen.writeString(value.getFormattedMessage());
    }

}
