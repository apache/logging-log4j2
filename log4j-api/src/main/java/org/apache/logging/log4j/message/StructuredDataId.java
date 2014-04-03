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
package org.apache.logging.log4j.message;

import java.io.Serializable;

/**
 * The StructuredData identifier.
 */
public class StructuredDataId implements Serializable {

    private static final String AT = "@";

    /**
     * RFC 5424 Time Quality.
     */
    public static final StructuredDataId TIME_QUALITY = new StructuredDataId("timeQuality", null,
        new String[]{"tzKnown", "isSynced", "syncAccuracy"});

    /**
     * RFC 5424 Origin.
     */
    public static final StructuredDataId ORIGIN = new StructuredDataId("origin", null,
        new String[]{"ip", "enterpriseId", "software", "swVersion"});

    /**
     * RFC 5424 Meta.
     */
    public static final StructuredDataId META = new StructuredDataId("meta", null,
        new String[]{"sequenceId", "sysUpTime", "language"});

    /**
     * Reserved enterprise number.
     */
    public static final int RESERVED = -1;

    private static final long serialVersionUID = 9031746276396249990L;
    private static final int MAX_LENGTH = 32;

    private final String name;
    private final int enterpriseNumber;
    private final String[] required;
    private final String[] optional;


    protected StructuredDataId(final String name, final String[] required, final String[] optional) {
        int index = -1;
        if (name != null) {
            if (name.length() > MAX_LENGTH) {
                throw new IllegalArgumentException(String.format("Length of id %s exceeds maximum of %d characters",
                        name, MAX_LENGTH));
            }
            index = name.indexOf(AT);
        }

        if (index > 0) {
            this.name = name.substring(0, index);
            this.enterpriseNumber = Integer.parseInt(name.substring(index + 1));
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
     * @param name             The name portion of the id.
     * @param enterpriseNumber The enterprise number.
     * @param required         The list of keys that are required for this id.
     * @param optional         The list of keys that are optional for this id.
     */
    public StructuredDataId(final String name, final int enterpriseNumber, final String[] required,
                            final String[] optional) {
        if (name == null) {
            throw new IllegalArgumentException("No structured id name was supplied");
        }
        if (name.contains(AT)) {
            throw new IllegalArgumentException("Structured id name cannot contain an '" + AT + '\'');
        }
        if (enterpriseNumber <= 0) {
            throw new IllegalArgumentException("No enterprise number was supplied");
        }
        this.name = name;
        this.enterpriseNumber = enterpriseNumber;
        final String id = enterpriseNumber < 0 ? name : name + AT + enterpriseNumber;
        if (id.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Length of id exceeds maximum of 32 characters: " + id);
        }
        this.required = required;
        this.optional = optional;
    }

    /**
     * Creates an id using another id to supply default values.
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
     * @param defaultId The default id to use if this StructuredDataId doesn't have a name.
     * @param enterpriseNumber The enterprise number.
     * @return a StructuredDataId.
     */
    public StructuredDataId makeId(final String defaultId, final int enterpriseNumber) {
        String id;
        String[] req;
        String[] opt;
        if (enterpriseNumber <= 0) {
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

        return new StructuredDataId(id, enterpriseNumber, req, opt);
    }

    /**
     * Returns a list of required keys.
     * @return a List of required keys or null if none have been provided.
     */
    public String[] getRequired() {
        return required;
    }

    /**
     * Returns a list of optional keys.
     * @return a List of optional keys or null if none have been provided.
     */
    public String[] getOptional() {
        return optional;
    }

    /**
     * Returns the StructuredDataId name.
     * @return the StructuredDataId name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the enterprise number.
     * @return the enterprise number.
     */
    public int getEnterpriseNumber() {
        return enterpriseNumber;
    }

    /**
     * Indicates if the id is reserved.
     * @return true if the id uses the reserved enterprise number, false otherwise.
     */
    public boolean isReserved() {
        return enterpriseNumber <= 0;
    }

    @Override
    public String toString() {
        return isReserved() ? name : name + AT + enterpriseNumber;
    }
}
