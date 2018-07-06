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
/**
 * SLF4J support. Note that this does indeed share the same package namespace as the one found in log4j-to-slf4j;
 * this is intentional. The two JARs should <em>not</em> be used at the same time! Thus, in an OSGi environment
 * where split packages are not allowed, this error is prevented due to both JARs sharing an exported package name.
 */
package org.apache.logging.slf4j;
