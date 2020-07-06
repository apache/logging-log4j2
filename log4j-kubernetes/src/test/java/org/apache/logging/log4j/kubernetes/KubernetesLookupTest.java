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
package org.apache.logging.log4j.kubernetes;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;

import static org.junit.Assert.assertEquals;

/**
 * Validate the Kubernetes Lookup.
 */
public class KubernetesLookupTest {

    private static final String localJson = "target/test-classes/localPod.json";
    private static final String clusterJson = "target/test-classes/clusterPod.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static URL masterUrl;

    @BeforeClass
    public static void beforeClass() throws Exception {
        masterUrl = new URL("http://localhost:443/");
    }

    @Test
    public void testLocal() throws Exception {
        Pod pod = objectMapper.readValue(new File(localJson), Pod.class);
        Namespace namespace = createNamespace();
        KubernetesLookup lookup = new KubernetesLookup(pod, namespace, masterUrl);
        try {
            assertEquals("Incorrect container name", "sampleapp", lookup.lookup("containerName"));
            assertEquals("Incorrect container id",
                    "docker://818b0098946c67e6ac56cb7c0934b7c2a9f50feb7244b422b2a7f566f7e5d0df",
                    lookup.lookup("containerId"));
            assertEquals("Incorrect host name", "docker-desktop", lookup.lookup("host"));
            assertEquals("Incorrect pod name", "sampleapp-584f99476d-mnrp4", lookup.lookup("podName"));
        } finally {
            lookup.clearInfo();;
        }
    }

    @Test
    public void testCluster() throws Exception {
        Pod pod = objectMapper.readValue(new File(clusterJson), Pod.class);
        Namespace namespace = createNamespace();
        KubernetesLookup lookup = new KubernetesLookup(pod, namespace, masterUrl);
        try {
            assertEquals("Incorrect container name", "platform-forms-service", lookup.lookup("containerName"));
            assertEquals("Incorrect container id",
                    "docker://2b7c2a93dfb48334aa549e29fdd38039ddd256eec43ba64c145fa4b75a1542f0",
                    lookup.lookup("containerId"));
            assertEquals("Incorrect host name", "k8s-tmpcrm-worker-s03-04", lookup.lookup("host"));
            assertEquals("Incorrect pod name", "platform-forms-service-primary-5ddfc4f9b8-kfpzv", lookup.lookup("podName"));
        } finally {
            lookup.clearInfo();
        }
    }

    private Namespace createNamespace() {
        Namespace namespace = new Namespace();
        ObjectMeta meta = new ObjectMeta();
        Map<String, String> annotations = new HashMap<>();
        annotations.put("test", "name");
        meta.setAnnotations(annotations);
        Map<String, String> labels = new HashMap<>();
        labels.put("ns", "my-namespace");
        meta.setLabels(labels);
        meta.setUid(UUID.randomUUID().toString());
        namespace.setMetadata(meta);
        return namespace;
    }
}
