package org.apache.logging.log4j.jackson.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.logging.log4j.core.LogEvent;

import java.util.Map;

public abstract class JsonModelLogEventWrapper {

    private LogEvent logEvent;
    private Map<String, String> additionalFields;

    public JsonModelLogEventWrapper(LogEvent logEvent, Map<String, String> additionalFields) {
        this.logEvent = logEvent;
        this.additionalFields = additionalFields;
    }

    @JsonIgnore
    public LogEvent getLogEvent() {
        return logEvent;
    }

    @JsonAnyGetter
    public Map<String, String> getAdditionalFields() {
        return this.additionalFields;
    }

}
