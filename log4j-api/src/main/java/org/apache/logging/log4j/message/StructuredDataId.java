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
package org.apache.logging.log4j.message;

import aQute.bnd.annotation.baseline.BaselineIgnore;
import com.google.errorprone.annotations.InlineMe;
import java.io.Serializable;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.Strings;

/**
 * The StructuredData identifier.
 */
public class StructuredDataId implements Serializable, StringBuilderFormattable {

    /**
     * RFC 5424 Time Quality.
     */
    public static final StructuredDataId TIME_QUALITY =
            new StructuredDataId("timeQuality", null, new String[] {"tzKnown", "isSynced", "syncAccuracy"});

    /**
     * RFC 5424 Origin.
     */
    public static final StructuredDataId ORIGIN =
            new StructuredDataId("origin", null, new String[] {"ip", "enterpriseId", "software", "swVersion"});

    /**
     * RFC 5424 Meta.
     */
    public static final StructuredDataId META =
            new StructuredDataId("meta", null, new String[] {"sequenceId", "sysUpTime", "language"});

    /**
     * Reserved enterprise number.
     */
    public static final String RESERVED = "-1";

    private static final long serialVersionUID = -8252896346202183738L;
    private static final int MAX_LENGTH = 32;
    private static final String AT_SIGN = "@";

    private final String name;
    private final String enterpriseNumber;
    private final String[] required;
    private final String[] optional;

    /**
     * Creates a StructuredDataId based on the name.
     * @param name The Structured Data Element name (maximum length is 32)
     * @since 2.9
     */
    public StructuredDataId(final String name) {
        this(name, null, null, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataId based on the name.
     * @param name The Structured Data Element name.
     * @param maxLength The maximum length of the name.
     * @since 2.9
     */
    public StructuredDataId(final String name, final int maxLength) {
        this(name, null, null, maxLength);
    }

    /**
     *
     * @param name The name portion of the id.
     * @param required The list of keys that are required for this id.
     * @param optional The list of keys that are optional for this id.
     */
    public StructuredDataId(final String name, final String[] required, final String[] optional) {
        this(name, required, optional, MAX_LENGTH);
    }

    /**
     * A Constructor that helps conformance to RFC 5424.
     *
     * @param name The name portion of the id.
     * @param required The list of keys that are required for this id.
     * @param optional The list of keys that are optional for this id.
     * @param maxLength The maximum length of the id's name.
     * @since 2.9
     */
    public StructuredDataId(final String name, final String[] required, final String[] optional, int maxLength) {
        int index = -1;
        if (name != null) {
            if (maxLength <= 0) {
                maxLength = MAX_LENGTH;
            }
            if (name.length() > maxLength) {
                throw new IllegalArgumentException(
                        String.format("Length of id %s exceeds maximum of %d characters", name, maxLength));
            }
            index = name.indexOf(AT_SIGN);
        }

        if (index > 0) {
            this.name = name.substring(0, index);
            this.enterpriseNumber = name.substring(index + 1).trim();
        } else {
            this.name = name;
            this.enterpriseNumber = RESERVED;
        }
        this.required = required;
        this.optional = optional;
    }

    /**
     * A Constructor that helps conformance to RFC 5424.
     *
     * @param name The name portion of the id.
     * @param enterpriseNumber The enterprise number.
     * @param required The list of keys that are required for this id.
     * @param optional The list of keys that are optional for this id.
     */
    public StructuredDataId(
            final String name, final String enterpriseNumber, final String[] required, final String[] optional) {
        this(name, enterpriseNumber, required, optional, MAX_LENGTH);
    }

    /**
     * A Constructor that helps conformance to RFC 5424.
     *
     * @param name The name portion of the id.
     * @param enterpriseNumber The enterprise number.
     * @param required The list of keys that are required for this id.
     * @param optional The list of keys that are optional for this id.
     * @deprecated Use {@link #StructuredDataId(String, String, String[], String[])} instead.
     */
    @Deprecated
    @InlineMe(replacement = "this(name, String.valueOf(enterpriseNumber), required, optional)")
    public StructuredDataId(
            final String name, final int enterpriseNumber, final String[] required, final String[] optional) {
        this(name, String.valueOf(enterpriseNumber), required, optional);
    }

    /**
     * A Constructor that helps conformance to RFC 5424.
     *
     * @param name The name portion of the id.
     * @param enterpriseNumber The enterprise number.
     * @param required The list of keys that are required for this id.
     * @param optional The list of keys that are optional for this id.
     * @param maxLength The maximum length of the StructuredData Id key.
     * @since 2.9
     */
    public StructuredDataId(
            final String name,
            final String enterpriseNumber,
            final String[] required,
            final String[] optional,
            final int maxLength) {
        if (name == null) {
            throw new IllegalArgumentException("No structured id name was supplied");
        }
        if (name.contains(AT_SIGN)) {
            throw new IllegalArgumentException("Structured id name cannot contain an " + Strings.quote(AT_SIGN));
        }
        if (RESERVED.equals(enterpriseNumber)) {
            throw new IllegalArgumentException("No enterprise number was supplied");
        }
        this.name = name;
        this.enterpriseNumber = enterpriseNumber;
        final String id = name + AT_SIGN + enterpriseNumber;
        if (maxLength > 0 && id.length() > maxLength) {
            throw new IllegalArgumentException("Length of id exceeds maximum of " + maxLength + " characters: " + id);
        }
        this.required = required;
        this.optional = optional;
    }

    /**
     * A Constructor that helps conformance to RFC 5424.
     *
     * @param name The name portion of the id.
     * @param enterpriseNumber The enterprise number.
     * @param required The list of keys that are required for this id.
     * @param optional The list of keys that are optional for this id.
     * @param maxLength The maximum length of the StructuredData Id key.
     * @since 2.9
     * @deprecated Use {@link #StructuredDataId(String, String, String[], String[], int)} instead.
     */
    @InlineMe(replacement = "this(name, String.valueOf(enterpriseNumber), required, optional, maxLength)")
    @Deprecated
    public StructuredDataId(
            final String name,
            final int enterpriseNumber,
            final String[] required,
            final String[] optional,
            final int maxLength) {
        this(name, String.valueOf(enterpriseNumber), required, optional, maxLength);
    }

    /**
     * Creates an id using another id to supply default values.
     *
     * @param id The original StructuredDataId.
     * @return the new StructuredDataId.
     */
    public StructuredDataId makeId(final StructuredDataId id) {
        if (id == null) {
            return this;
        }
        return makeId(id.getName(), id.getEnterpriseNumber());
    }

    /**
     * Creates an id based on the current id.
     *
     * @param defaultId The default id to use if this StructuredDataId doesn't have a name.
     * @param anEnterpriseNumber The enterprise number.
     * @return a StructuredDataId.
     */
    public StructuredDataId makeId(final String defaultId, final String anEnterpriseNumber) {
        String id;
        String[] req;
        String[] opt;
        if (RESERVED.equals(anEnterpriseNumber)) {
            return this;
        }
        if (this.name != null) {
            id = this.name;
            req = this.required;
            opt = this.optional;
        } else {
            id = defaultId;
            req = null;
            opt = null;
        }

        return new StructuredDataId(id, anEnterpriseNumber, req, opt);
    }

    /**
     * Creates an id based on the current id.
     *
     * @param defaultId The default id to use if this StructuredDataId doesn't have a name.
     * @param anEnterpriseNumber The enterprise number.
     * @return a StructuredDataId.
     * @deprecated Use {@link StructuredDataId#makeId(String, String)} instead
     */
    @Deprecated
    // This method should have been `final` from the start, we don't expect anyone to override it.
    @BaselineIgnore("2.22.0")
    @InlineMe(replacement = "this.makeId(defaultId, String.valueOf(anEnterpriseNumber))")
    public final StructuredDataId makeId(final String defaultId, final int anEnterpriseNumber) {
        return makeId(defaultId, String.valueOf(anEnterpriseNumber));
    }

    /**
     * Returns a list of required keys.
     *
     * @return a List of required keys or null if none have been provided.
     */
    public String[] getRequired() {
        return required;
    }

    /**
     * Returns a list of optional keys.
     *
     * @return a List of optional keys or null if none have been provided.
     */
    public String[] getOptional() {
        return optional;
    }

    /**
     * Returns the StructuredDataId name.
     *
     * @return the StructuredDataId name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the enterprise number.
     *
     * @return the enterprise number.
     */
    public String getEnterpriseNumber() {
        return enterpriseNumber;
    }

    /**
     * Indicates if the id is reserved.
     *
     * @return true if the id uses the reserved enterprise number, false otherwise.
     */
    public boolean isReserved() {
        return RESERVED.equals(enterpriseNumber);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(name.length() + 10);
        formatTo(sb);
        return sb.toString();
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        if (isReserved()) {
            buffer.append(name);
        } else {
            buffer.append(name).append(AT_SIGN).append(enterpriseNumber);
        }
    }
}
