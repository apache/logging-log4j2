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
public class Network {

    @JsonProperty("IPAMConfig")
    private IPAMConfig ipamConfig;

    @JsonProperty("Links")
    private String links;

    @JsonProperty("Aliases")
    private String[] aliases;

    @JsonProperty("NetworkID")
    private String networkId;

    @JsonProperty("EndpointID")
    private String endpointId;

    @JsonProperty("Gateway")
    private String gateway;

    @JsonProperty("IPAddress")
    private String ipAddress;

    @JsonProperty("IPPrefixLen")
    private Integer ipPrefixLen;

    @JsonProperty("IPv6Gateway")
    private String ipv6Gateway;

    @JsonProperty("GlobalIPv6Address")
    private String globalIPv6Address;

    @JsonProperty("GlobalIPv6PrefixLen")
    private Integer globalIPv6PrefixLen;

    @JsonProperty("MacAddress")
    private String macAddress;

    @JsonProperty("DriverOpts")
    private String driverOpts;

    public IPAMConfig getIpamConfig() {
        return ipamConfig;
    }

    public void setIpamConfig(final IPAMConfig ipamConfig) {
        this.ipamConfig = ipamConfig;
    }

    public String getLinks() {
        return links;
    }

    public void setLinks(final String links) {
        this.links = links;
    }

    public String[] getAliases() {
        return aliases;
    }

    public void setAliases(final String[] aliases) {
        this.aliases = aliases;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(final String networkId) {
        this.networkId = networkId;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(final String endpointId) {
        this.endpointId = endpointId;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(final String gateway) {
        this.gateway = gateway;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getIpPrefixLen() {
        return ipPrefixLen;
    }

    public void setIpPrefixLen(final Integer ipPrefixLen) {
        this.ipPrefixLen = ipPrefixLen;
    }

    public String getIpv6Gateway() {
        return ipv6Gateway;
    }

    public void setIpv6Gateway(final String ipv6Gateway) {
        this.ipv6Gateway = ipv6Gateway;
    }

    public String getGlobalIPv6Address() {
        return globalIPv6Address;
    }

    public void setGlobalIPv6Address(final String globalIPv6Address) {
        this.globalIPv6Address = globalIPv6Address;
    }

    public Integer getGlobalIPv6PrefixLen() {
        return globalIPv6PrefixLen;
    }

    public void setGlobalIPv6PrefixLen(final Integer globalIPv6PrefixLen) {
        this.globalIPv6PrefixLen = globalIPv6PrefixLen;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(final String macAddress) {
        this.macAddress = macAddress;
    }

    public String getDriverOpts() {
        return driverOpts;
    }

    public void setDriverOpts(final String driverOpts) {
        this.driverOpts = driverOpts;
    }
}
