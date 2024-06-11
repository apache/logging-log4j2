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
package example;

import java.net.InetSocketAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;

public class CustomMessageExample {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        InetSocketAddress socketAddress = InetSocketAddress.createUnresolved("192.0.2.17", 1234);
        LoginFailureEvent event = new LoginFailureEvent("root", socketAddress);
        LOGGER.info(event);
    }

    // tag::loginFailure[]
    record LoginFailureEvent(String userName, InetSocketAddress remoteAddress)
            implements Message, StringBuilderFormattable { // <1>

        @Override
        public void formatTo(StringBuilder buffer) { // <2>
            buffer.append("Connection closed by authenticating user ")
                    .append(userName())
                    .append(" ")
                    .append(remoteAddress().getHostName())
                    .append(" port ")
                    .append(remoteAddress().getPort())
                    .append(" [preauth]");
        }

        @Override
        public String getFormattedMessage() { // <3>
            StringBuilder buffer = new StringBuilder();
            formatTo(buffer);
            return buffer.toString();
        }
        // end::loginFailure[]

        @Override
        public String getFormat() {
            return "";
        }

        @Override
        public Object[] getParameters() {
            return null;
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }
        // tag::loginFailure[]

    }
    // end::loginFailure[]
}
