package org.slf4j.helpers;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.slf4j.ext.EventData;

import java.util.Map;

/**
 *
 */
public class EventDataConverter {

    public Message convertEvent(final String s1, final Object[] objects, final Throwable throwable) {
        Message msg;
        try {
            final EventData data = (objects != null && objects[0] instanceof EventData) ? (EventData) objects[0] :
                new EventData(s1);
            msg = new StructuredDataMessage(data.getEventId(), data.getMessage(), data.getEventType());
            for (final Map.Entry entry : data.getEventMap().entrySet()) {
                final String key = entry.getKey().toString();
                if (EventData.EVENT_TYPE.equals(key) || EventData.EVENT_ID.equals(key) ||
                    EventData.EVENT_MESSAGE.equals(key)) {
                    continue;
                }
                ((StructuredDataMessage) msg).put(entry.getKey().toString(), entry.getValue().toString());
            }
        } catch (final Exception ex) {
            msg = new ParameterizedMessage(s1, objects, throwable);
        }
        return msg;
    }
}
