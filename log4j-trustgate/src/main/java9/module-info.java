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
module org.apache.logging.log4j.trustgate {
    exports org.apache.logging.log4j.trustgate;
    exports org.apache.logging.log4j.trustgate.rules;
    exports org.apache.logging.log4j.trustgate.spi;
    uses org.apache.logging.log4j.trustgate.spi.InputSanitizer;
    uses org.apache.logging.log4j.trustgate.spi.ValidationRule;
    provides org.apache.logging.log4j.trustgate.spi.InputSanitizer with org.apache.logging.log4j.trustgate.DefaultInputSanitizer;
    provides org.apache.logging.log4j.trustgate.spi.ValidationRule with
            org.apache.logging.log4j.trustgate.rules.JndiSchemeValidationRule,
            org.apache.logging.log4j.trustgate.rules.UriSchemeValidationRule,
            org.apache.logging.log4j.trustgate.rules.LookupPatternValidationRule,
            org.apache.logging.log4j.trustgate.rules.RecursiveLookupValidationRule,
            org.apache.logging.log4j.trustgate.rules.PropertyKeyValidationRule;
}
