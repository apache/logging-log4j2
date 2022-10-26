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
package org.apache.logging.log4j.migration.jcl;

public class JclTypes {

    public static final String LOG = "org/apache/commons/logging/Log";
    public static final String LOGFACTORY = "org/apache/commons/logging/LogFactory";
    public static final String LOGFACTORY_DESC = "Lorg/apache/commons/logging/LogFactory;";

    // Supported LogFactory methods
    public static final String GET_FACTORY_NAME = "getFactory";
    public static final String GET_FACTORY_DESC = "()Lorg/apache/commons/logging/LogFactory;";
    public static final String GET_LOG_NAME = "getLog";
    public static final String GET_LOG_STRING_DESC = "(Ljava/lang/String;)Lorg/apache/commons/logging/Log;";
    public static final String GET_LOG_CLASS_DESC = "(Ljava/lang/Class;)Lorg/apache/commons/logging/Log;";
    public static final String GET_INSTANCE_NAME = "getInstance";
    public static final String GET_INSTANCE_STRING_DESC = "(Ljava/lang/String;)Lorg/apache/commons/logging/Log;";
    public static final String GET_INSTANCE_CLASS_DESC = "(Ljava/lang/Class;)Lorg/apache/commons/logging/Log;";

    private JclTypes() {
        // prevents instantiation
    }
}
