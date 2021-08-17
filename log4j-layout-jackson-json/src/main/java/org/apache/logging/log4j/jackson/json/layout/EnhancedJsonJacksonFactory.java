package org.apache.logging.log4j.jackson.json.layout;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.jackson.AbstractEnhancedJacksonFactory;
import org.apache.logging.log4j.jackson.json.Log4jJsonObjectMapper;

class EnhancedJsonJacksonFactory extends AbstractEnhancedJacksonFactory {

    protected ObjectMapper newObjectMapper() {
        return new Log4jJsonObjectMapper(false, false, false, false);
    }

    protected PrettyPrinter newCompactPrinter() {
        return new MinimalPrettyPrinter();
    }

    protected PrettyPrinter newPrettyPrinter() {
        return new DefaultPrettyPrinter();
    }
}
