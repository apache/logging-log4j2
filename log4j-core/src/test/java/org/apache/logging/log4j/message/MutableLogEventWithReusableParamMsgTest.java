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
package org.apache.logging.log4j.message;

import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.junit.Test;

/**
 * LOG4J2-1409
 */
// test must be in log4j-core but in org.apache.logging.log4j.message package because it calls package-private methods
public class MutableLogEventWithReusableParamMsgTest {
    @Test
    public void testInteractionWithReusableParameterizedMessage() {
        final MutableLogEvent evt = new MutableLogEvent();
        final ReusableParameterizedMessage msg = new ReusableParameterizedMessage();
        msg.set("Hello {} {} {}", 1, 2, 3);
        evt.setMessage(msg);
        evt.clear();

        msg.set("Hello {}", new Object[]{1});
        evt.setMessage(msg);
        evt.clear();

        msg.set("Hello {}", 1);
        evt.setMessage(msg);
        evt.clear();

        // Uncomment out this log event and the params gets reset correctly (No exception occurs)
        //        msg.set("Hello {}", 1);
        //        evt.setMessage(msg);
        //        evt.clear();

        // Exception at this log event - as the params is set to 1!
        msg.set("Hello {} {} {}", 1, 2, 3);
        evt.setMessage(msg);
        evt.clear();
    }
}
