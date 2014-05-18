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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 */
public class ListOfMapEntrySerializer extends StdSerializer<Map> {

    protected ListOfMapEntrySerializer() {
        super(Map.class);
    }

    @Override
    public void serialize(final Map map, final JsonGenerator jgen, final SerializerProvider provider) throws IOException, JsonGenerationException {
        final Set<Entry<String, String>> entrySet = map.entrySet();
        final MapEntry[] pairs = new MapEntry[entrySet.size()];
        int i = 0;
        for (final Entry<String, String> entry : entrySet) {
            pairs[i++] = new MapEntry(entry.getKey(), entry.getValue());
        }
        jgen.writeObject(pairs);
    }

}
