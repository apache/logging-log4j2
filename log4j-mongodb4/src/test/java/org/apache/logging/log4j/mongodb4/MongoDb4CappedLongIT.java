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
package org.apache.logging.log4j.mongodb4;

import com.mongodb.client.MongoClient;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

@UsingMongoDb4
@LoggerContextSource("MongoDb4CappedLongIT.xml")
// Print debug status logger output upon failure
@UsingStatusListener
class MongoDb4CappedLongIT extends AbstractMongoDb4CappedIT {

    @Test
    @Override
    protected void test(LoggerContext ctx, MongoClient mongoClient) {
        super.test(ctx, mongoClient);
    }
}
