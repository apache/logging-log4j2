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
package org.apache.logging.log4j.docker;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.AbstractLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.docker.model.Container;
import org.apache.logging.log4j.docker.model.Network;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 */
@Plugin(name = "docker", category = StrLookup.CATEGORY)
public class DockerLookup extends AbstractLookup {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String DOCKER_URI = "DOCKER_URI";
    private static final String HTTP = "http";
    private final Container container;

    public DockerLookup() {
        String baseUri = System.getenv(DOCKER_URI);
        if (baseUri == null) {
            PropertiesUtil props = PropertiesUtil.getProperties();
            baseUri = props.getStringProperty(DOCKER_URI);
        }
        if (baseUri == null) {
            LOGGER.warn("No Docker URI provided. Docker information is unavailable");
            container = null;
            return;
        }
        Container current = null;
        try {
            URL url= new URL(baseUri + "/containers/json");
            String hostName = NetUtils.getLocalHostname();
            String macAddr = NetUtils.getMacAddressString();

            if (url.getProtocol().equals(HTTP)) {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Container> containerList = objectMapper.readValue(url, new TypeReference<List<Container>>(){});

                for (Container container : containerList) {
                    if (macAddr != null && container.getNetworkSettings() != null) {
                        Map<String, Network> networks = container.getNetworkSettings().getNetworks();
                        if (networks != null) {
                            for (Network network: networks.values()) {
                                if (macAddr.equals(network.getMacAddress())) {
                                    current = container;
                                    break;
                                }
                            }
                        }
                    }
                    if (current != null) {
                        break;
                    }
                }
            }
            if (current == null) {
                LOGGER.warn("Unable to determine current container");
            }
        } catch (IOException ioe) {
            LOGGER.warn("Unable to read container information: " + ioe.getMessage());
        }
        container = current;
    }

    @Override
    public String lookup(LogEvent event, String key) {
        if (container == null) {
            return null;
        }
        switch (key) {
            case "shortContainerId": {
                return container.getId().substring(0, 12);
            }
            case "containerId": {
                return container.getId();
            }
            case "containerName": {
                if (container.getNames().size() > 1) {
                    return container.getNames().toString();
                }
                return container.getNames().get(0);
            }
            case "shortImageId": {
                return container.getImageId().substring(0, 12);
            }
            case "imageId": {
                return container.getImageId();
            }
            case "imageName": {
                return container.getImage();
            }
            default:
                return null;
        }
    }
}
