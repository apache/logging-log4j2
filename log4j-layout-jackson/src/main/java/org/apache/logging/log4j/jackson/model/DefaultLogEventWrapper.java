package org.apache.logging.log4j.jackson.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.logging.log4j.core.LogEvent;

import java.util.Map;

public class DefaultLogEventWrapper extends JsonModelLogEventWrapper {

    public DefaultLogEventWrapper(LogEvent event, Map<String, String> additionalFields) {
        super(event, additionalFields);
    }

    @JsonUnwrapped
    public LogEvent getLogEvent() {
        return super.getLogEvent();
    }

    @JsonAnyGetter
    public Map<String, String> getAdditionalFields() {
        return super.getAdditionalFields();
    }
}
