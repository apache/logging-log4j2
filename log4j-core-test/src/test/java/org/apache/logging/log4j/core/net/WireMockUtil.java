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
package org.apache.logging.log4j.core.net;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.after;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.before;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToDateTime;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.stubbing.StubImport.stubImport;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.IF_MODIFIED_SINCE;
import static com.google.common.net.HttpHeaders.LAST_MODIFIED;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.stubbing.StubImport;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class WireMockUtil {

    private static final DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);

    /**
     * Establishes a set of mapping to serve a file
     *
     * @param urlPath The URL path of the served file
     * @param credentials The credentials to use for authentication
     * @param body The body of the file
     * @param contentType The MIME content type of the file
     * @param lastModified The last modification date of the file
     * @return A set of mappings
     */
    public static StubImport createMapping(
            String urlPath, BasicCredentials credentials, byte[] body, String contentType, ZonedDateTime lastModified) {
        int idx = urlPath.lastIndexOf('/');
        String fileName = idx == -1 ? urlPath : urlPath.substring(idx + 1);
        return stubImport()
                // Lack of authentication data
                .stub(get(anyUrl())
                        .withHeader(AUTHORIZATION, absent())
                        .willReturn(aResponse().withStatus(401).withStatusMessage("Not Authenticated")))
                // Wrong authentication data
                .stub(get(anyUrl())
                        .withHeader(AUTHORIZATION, notContaining(credentials.asAuthorizationHeaderValue()))
                        .willReturn(aResponse().withStatus(403).withStatusMessage("Not Authorized")))
                // Serves the file
                .stub(get(urlPath)
                        .withBasicAuth(credentials.username, credentials.password)
                        .withHeader(IF_MODIFIED_SINCE, before(lastModified).or(absent()))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBodyFile(fileName)
                                .withBody(body)
                                .withHeader(LAST_MODIFIED, formatter.format(lastModified))
                                .withHeader(CONTENT_TYPE, contentType)))
                // The file was not updated since lastModified
                .stub(get(urlPath)
                        .withBasicAuth(credentials.username, credentials.password)
                        .withHeader(IF_MODIFIED_SINCE, after(lastModified).or(equalToDateTime(lastModified)))
                        .willReturn(
                                aResponse().withStatus(304).withHeader(LAST_MODIFIED, formatter.format(lastModified))))
                .build();
    }
}
