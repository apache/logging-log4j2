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
import java.util.List;
import java.util.Map;

/**
 * Definition of a Docker Container
 */
public class Container {
    @JsonProperty("Id")
    private String id;

    @JsonProperty("Names")
    private List<String> names;

    @JsonProperty("Path")
    private String path;

    @JsonProperty("Args")
    private String[] args;

    @JsonProperty("Image")
    private String image;

    @JsonProperty("ImageID")
    private String imageId;

    @JsonProperty("Command")
    private String command;

    @JsonProperty("Created")
    private Long created;

    @JsonProperty("Ports")
    private List<PortDefinition> ports;

    @JsonProperty("Labels")
    private Map<String, String> labels;

    @JsonProperty("State")
    private String state;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("HostConfig")
    private HostConfig hostConfig;

    @JsonProperty("NetworkSettings")
    private NetworkSettings networkSettings;

    @JsonProperty("Mounts")
    private List<Mount> mounts;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(final List<String> names) {
        this.names = names;
    }

    public String getImage() {
        return image;
    }

    public void setImage(final String image) {
        this.image = image;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(final String imageId) {
        this.imageId = imageId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(final String command) {
        this.command = command;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(final Long created) {
        this.created = created;
    }

    public List<PortDefinition> getPorts() {
        return ports;
    }

    public void setPorts(final List<PortDefinition> ports) {
        this.ports = ports;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(final Map<String, String> labels) {
        this.labels = labels;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public HostConfig getHostConfig() {
        return hostConfig;
    }

    public void setHostConfig(final HostConfig hostConfig) {
        this.hostConfig = hostConfig;
    }

    public NetworkSettings getNetworkSettings() {
        return networkSettings;
    }

    public void setNetworkSettings(final NetworkSettings networkSettings) {
        this.networkSettings = networkSettings;
    }

    public List<Mount> getMounts() {
        return mounts;
    }

    public void setMounts(final List<Mount> mounts) {
        this.mounts = mounts;
    }
}
