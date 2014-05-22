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
package org.apache.logging.log4j.samples.dto;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.util.UuidUtil;

/**
 *
 */
public final class RequestContext {

    private RequestContext() {
    }
    // Unique token to identify this request.
    public static final String REQUEST_ID = "id";
    // Token used to correlate multiple events within the request.
    public static final String TRANSACTION_ID = "transId";
    // The requested resource.
    public static final String REQUEST_URI = "requestURI";
    // Identify the user's session - should never contain the HTTP SessionId.
    public static final String SESSION_ID = "sessionId";
    // The id the user logged in with.
    public static final String LOGIN_ID = "loginId";
    // The id the system associates with the user.
    public static final String USER_ID = "userId";
    // user, admin, etc.
    public static final String USER_TYPE = "userType";
    // client id in a multi-tenant application
    public static final String CLIENT_ID = "clientId";
    // The user's ipAddress.
    public static final String IP_ADDRESS = "ipAddress";
    // The name of the product.
    public static final String PRODUCT_NAME = "productName";
    // The product version.
    public static final String PRODUCT_VERSION = "productVersion";
    // The users locale.
    public static final String LOCALE = "locale";
    // prod, preprod, beta, dev, etc.
    public static final String REGION = "region";
    // The user agent string from the browser.
    public static final String USER_AGENT = "userAgent";

    public static void initialize() {
        ThreadContext.clearMap();
        ThreadContext.put(REQUEST_ID, UuidUtil.getTimeBasedUuid().toString());
    }

    public static String getId() {
        return ThreadContext.get(REQUEST_ID);
    }

    public static void setSessionId(final String id) {
        ThreadContext.put(SESSION_ID, id);
    }

    public static String getSessionId() {
        return ThreadContext.get(SESSION_ID);
    }

    public static void setTransId(final String id) {
        ThreadContext.put(TRANSACTION_ID,  id);
    }

    public static String getTransId() {
        return ThreadContext.get(TRANSACTION_ID);
    }

    public static void setRequestURI(final String URI) {
        ThreadContext.put(REQUEST_URI,  URI);
    }

    public static String getRequestURI() {
        return ThreadContext.get(REQUEST_URI);
    }

    public static void setLoginId(final String id) {
        ThreadContext.put(LOGIN_ID,  id);
    }

    public static String getLoginId() {
        return ThreadContext.get(LOGIN_ID);
    }

    public static void setUserId(final String id) {
        ThreadContext.put(USER_ID,  id);
    }

    public static String getUserId() {
        return ThreadContext.get(USER_ID);
    }

    public static void setUserType(final String type) {
        ThreadContext.put(USER_TYPE,  type);
    }

    public static String getUserType() {
        return ThreadContext.get(USER_TYPE);
    }

    public static void setClientId(final String id) {
        ThreadContext.put(CLIENT_ID,  id);
    }

    public static String getClientId() {
        return ThreadContext.get(CLIENT_ID);
    }

    public static void setIpAddress(final String addr) {
        ThreadContext.put(IP_ADDRESS,  addr);
    }

    public static String getIpAddress() {
        return ThreadContext.get(IP_ADDRESS);
    }

    public static void setProductName(final String productName) {
        ThreadContext.put(PRODUCT_NAME, productName);
    }

    public static String getProductName() {
        return ThreadContext.get(PRODUCT_NAME);
    }


    public static void setProductVersion(final String productVersion) {
        ThreadContext.put(PRODUCT_VERSION, productVersion);
    }

    public static String getProductVersion() {
        return ThreadContext.get(PRODUCT_VERSION);
    }

    public static void setLocale(final String locale) {
        ThreadContext.put(LOCALE, locale);
    }

    public static String getLocale() {
        return ThreadContext.get(LOCALE);
    }

    public static void setRegion(final String region) {
        ThreadContext.put(REGION, region);
    }

    public static String getRegion() {
        return ThreadContext.get(REGION);
    }

    public static void setUserAgent(final String agent) {
        ThreadContext.put(USER_AGENT, agent);
    }

    public static String getUserAgent() {
        return ThreadContext.get(USER_AGENT);
    }

}
