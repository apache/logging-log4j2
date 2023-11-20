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
import java.util.Map;

/**
 *
 */
public class NetworkSettings {

    @JsonProperty("Networks")
    private Map<String, Network> networks;

    @JsonProperty("Bridge")
    private String bridge;

    @JsonProperty("SandboxID")
    private String sandboxId;

    @JsonProperty("HairpinMode")
    private boolean hairpinMode;

    @JsonProperty("LinkLocalIPv6Address")
    private String linkLocalIPv6Address;

    @JsonProperty("LinkLocalIPv6PrefixLen")
    private int linkLocalIPv6PrefixLen;

    @JsonProperty("Ports")
    private Map<String, String> ports;

    @JsonProperty("SandboxKey")
    private String sandboxKey;

    @JsonProperty("SecondaryIPAddresses")
    private String secondaryIPaddresses;

    @JsonProperty("EndpointID")
    private String endpointId;

    @JsonProperty("Gateway")
    private String gateway;

    @JsonProperty("GlobalIPv6Address")
    private String globalIPv6Address;

    @JsonProperty("GlobalIPv6PrefixLen")
    private int globalIPv6PrefixLen;

    @JsonProperty("IPAddress")
    private String ipAddress;

    @JsonProperty("IPPrefixLen")
    private int ipPrefixLen;

    @JsonProperty("IPv6Gateway")
    private String ipv6Gateway;

    @JsonProperty("MacAddress")
    private String macAddress;

    public Map<String, Network> getNetworks() {
        return networks;
    }

    public void setNetworks(final Map<String, Network> networks) {
        this.networks = networks;
    }

    public String getBridge() {
        return bridge;
    }

    public void setBridge(final String bridge) {
        this.bridge = bridge;
    }

    public String getSandboxId() {
        return sandboxId;
    }

    public void setSandboxId(final String sandboxId) {
        this.sandboxId = sandboxId;
    }

    public boolean isHairpinMode() {
        return hairpinMode;
    }

    public void setHairpinMode(final boolean hairpinMode) {
        this.hairpinMode = hairpinMode;
    }

    public String getLinkLocalIPv6Address() {
        return linkLocalIPv6Address;
    }

    public void setLinkLocalIPv6Address(final String linkLocalIPv6Address) {
        this.linkLocalIPv6Address = linkLocalIPv6Address;
    }

    public int getLinkLocalIPv6PrefixLen() {
        return linkLocalIPv6PrefixLen;
    }

    public void setLinkLocalIPv6PrefixLen(final int linkLocalIPv6PrefixLen) {
        this.linkLocalIPv6PrefixLen = linkLocalIPv6PrefixLen;
    }

    public Map<String, String> getPorts() {
        return ports;
    }

    public void setPorts(final Map<String, String> ports) {
        this.ports = ports;
    }

    public String getSandboxKey() {
        return sandboxKey;
    }

    public void setSandboxKey(final String sandboxKey) {
        this.sandboxKey = sandboxKey;
    }

    public String getSecondaryIPaddresses() {
        return secondaryIPaddresses;
    }

    public void setSecondaryIPaddresses(final String secondaryIPaddresses) {
        this.secondaryIPaddresses = secondaryIPaddresses;
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

    public String getGlobalIPv6Address() {
        return globalIPv6Address;
    }

    public void setGlobalIPv6Address(final String globalIPv6Address) {
        this.globalIPv6Address = globalIPv6Address;
    }

    public int getGlobalIPv6PrefixLen() {
        return globalIPv6PrefixLen;
    }

    public void setGlobalIPv6PrefixLen(final int globalIPv6PrefixLen) {
        this.globalIPv6PrefixLen = globalIPv6PrefixLen;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getIpPrefixLen() {
        return ipPrefixLen;
    }

    public void setIpPrefixLen(final int ipPrefixLen) {
        this.ipPrefixLen = ipPrefixLen;
    }

    public String getIpv6Gateway() {
        return ipv6Gateway;
    }

    public void setIpv6Gateway(final String ipv6Gateway) {
        this.ipv6Gateway = ipv6Gateway;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(final String macAddress) {
        this.macAddress = macAddress;
    }
}
