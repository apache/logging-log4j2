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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * A collection of StructuredDataMessages.
 */
public class StructuredDataCollectionMessage implements StringBuilderFormattable,
        MessageCollectionMessage<StructuredDataMessage> {
    private static final long serialVersionUID = 5725337076388822924L;

    private List<StructuredDataMessage> structuredDataMessageList;

    public StructuredDataCollectionMessage(List<StructuredDataMessage> messages) {
        this.structuredDataMessageList = messages;
    }

    @Override
    public Iterator<StructuredDataMessage> iterator() {
        return structuredDataMessageList.iterator();
    }

    @Override
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        formatTo(sb);
        return sb.toString();
    }

    @Override
    public String getFormat() {
        StringBuilder sb = new StringBuilder();
        for (StructuredDataMessage msg : structuredDataMessageList) {
            if (msg.getFormat() != null) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(msg.getFormat());
            }
        }
        return sb.toString();
    }

    @Override
    public void formatTo(StringBuilder buffer) {
        for (StructuredDataMessage msg : structuredDataMessageList) {
            msg.formatTo(buffer);
        }
    }

    @Override
    public Object[] getParameters() {
        List<Object[]> objectList = new ArrayList<>();
        int count = 0;
        for (StructuredDataMessage msg : structuredDataMessageList) {
            Object[] objects = msg.getParameters();
            if (objects != null) {
                objectList.add(objects);
                count += objects.length;
            }
        }
        Object[] objects = new Object[count];
        int index = 0;
        for (Object[] objs : objectList) {
           for (Object obj : objs) {
               objects[index++] = obj;
           }
        }
        return objects;
    }

    @Override
    public Throwable getThrowable() {
        for (StructuredDataMessage msg : structuredDataMessageList) {
            Throwable t = msg.getThrowable();
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    @Override
    public StackTraceElement getSource() {
        for (StructuredDataMessage msg : structuredDataMessageList) {
            StackTraceElement t = msg.getSource();
            if (t != null) {
                return t;
            }
        }
        return null;
    }
}
