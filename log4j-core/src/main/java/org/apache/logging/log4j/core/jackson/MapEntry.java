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
package org.apache.logging.log4j.core.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.apache.logging.log4j.util.Strings;

/**
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 * <p>
 * Used to represent map entries in a generic fashion because the default Jackson behavior uses the key as the element tag. Using the key as
 * an element/property name would mean that you cannot have a generic JSON/XML schema for all log event.
 * </p>
 */
@JsonPropertyOrder({"key", "value"})
final class MapEntry {

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    private String key;

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    private String value;

    @JsonCreator
    public MapEntry(@JsonProperty("key") final String key, @JsonProperty("value") final String value) {
        this.setKey(key);
        this.setValue(value);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MapEntry)) {
            return false;
        }
        final MapEntry other = (MapEntry) obj;
        if (this.getKey() == null) {
            if (other.getKey() != null) {
                return false;
            }
        } else if (!this.getKey().equals(other.getKey())) {
            return false;
        }
        if (this.getValue() == null) {
            if (other.getValue() != null) {
                return false;
            }
        } else if (!this.getValue().equals(other.getValue())) {
            return false;
        }
        return true;
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.getKey() == null) ? 0 : this.getKey().hashCode());
        result = prime * result
                + ((this.getValue() == null) ? 0 : this.getValue().hashCode());
        return result;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Strings.EMPTY + this.getKey() + "=" + this.getValue();
    }
}
