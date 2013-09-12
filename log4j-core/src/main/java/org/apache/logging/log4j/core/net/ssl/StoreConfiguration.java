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
package org.apache.logging.log4j.core.net.ssl;

import org.apache.logging.log4j.status.StatusLogger;

/**
 *
 */
public class StoreConfiguration {
    protected static final StatusLogger LOGGER = StatusLogger.getLogger();

    private String location;
    private String password;

    public StoreConfiguration(String location, String password) {
        this.location = location;
        this.password = password;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPassword() {
        return password;
    }

    public char[] getPasswordAsCharArray() {
        if (password == null)
            return null;
        else
            return password.toCharArray();
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean equals(StoreConfiguration config) {
        if (config == null)
            return false;

        boolean locationEquals = false;
        boolean passwordEquals = false;

        if (location != null)
            locationEquals = location.equals(config.location);
        else
            locationEquals = location == config.location;

        if (password != null)
            passwordEquals = password.equals(config.password);
        else
            passwordEquals = password == config.password;

        return locationEquals && passwordEquals;
    }

    protected void load() throws StoreConfigurationException {
    }
}
