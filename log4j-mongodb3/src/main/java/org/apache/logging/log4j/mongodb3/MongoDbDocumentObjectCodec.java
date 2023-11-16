/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.mongodb3;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;

final class MongoDbDocumentObjectCodec implements Codec<MongoDbDocumentObject> {

    private final Codec<Document> documentCodec = new DocumentCodec();

    @Override
    public void encode(
            final BsonWriter writer, final MongoDbDocumentObject value, final EncoderContext encoderContext) {
        documentCodec.encode(writer, value.unwrap(), encoderContext);
    }

    @Override
    public Class<MongoDbDocumentObject> getEncoderClass() {
        return MongoDbDocumentObject.class;
    }

    @Override
    public MongoDbDocumentObject decode(final BsonReader reader, final DecoderContext decoderContext) {
        final MongoDbDocumentObject object = new MongoDbDocumentObject();
        documentCodec.decode(reader, decoderContext).entrySet().stream()
                .forEach(entry -> object.set(entry.getKey(), entry.getValue()));
        return object;
    }
}
