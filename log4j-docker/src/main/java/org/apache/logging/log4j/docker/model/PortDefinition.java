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
package org.apache.logging.log4j.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class PortDefinition {

    @JsonProperty("IP")
    private String ip;

    @JsonProperty("PrivatePort")
    private Integer privatePort;

    @JsonProperty("PublicPort")
    private Integer publicPort;

    @JsonProperty("Type")
    private String type;

    public String getIp() {
        return ip;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }

    public Integer getPrivatePort() {
        return privatePort;
    }

    public void setPrivatePort(final Integer privatePort) {
        this.privatePort = privatePort;
    }

    public Integer getPublicPort() {
        return publicPort;
    }

    public void setPublicPort(final Integer publicPort) {
        this.publicPort = publicPort;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }
}
