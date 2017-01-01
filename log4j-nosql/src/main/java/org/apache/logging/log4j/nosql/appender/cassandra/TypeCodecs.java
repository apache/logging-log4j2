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
package org.apache.logging.log4j.nosql.appender.cassandra;

import java.nio.ByteBuffer;

import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.exceptions.InvalidTypeException;
import org.apache.logging.log4j.util.Strings;

/**
 * Custom TypeCodecs for use with the Datastax Cassandra driver.
 */
public final class TypeCodecs {

    public static void registerCustomCodecs() {
        CodecRegistry.DEFAULT_INSTANCE.register(new LongTimestampCodec());
    }

    /**
     * TypeCodec that allows a long value to be used as a timestamp.
     */
    public static class LongTimestampCodec extends TypeCodec.PrimitiveLongCodec {

        private LongTimestampCodec() {
            super(DataType.timestamp());
        }

        @Override
        public ByteBuffer serializeNoBoxing(final long v, final ProtocolVersion protocolVersion) {
            final ByteBuffer bb = ByteBuffer.allocate(8);
            bb.putLong(v);
            return bb;
        }

        @Override
        public long deserializeNoBoxing(final ByteBuffer v, final ProtocolVersion protocolVersion) {
            if (v == null || v.remaining() == 0) {
                return 0;
            }
            if (v.remaining() != 8) {
                throw new InvalidTypeException("Expected an 8 byte value, but got " + v.remaining() + " bytes");
            }
            return v.getLong(v.position());
        }

        @Override
        public Long parse(final String value) throws InvalidTypeException {
            return Strings.isEmpty(value) || "NULL".equalsIgnoreCase(value) ? null : Long.parseLong(value);
        }

        @Override
        public String format(final Long value) throws InvalidTypeException {
            return value == null ? "NULL" : value.toString();
        }
    }

}
