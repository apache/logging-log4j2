package org.apache.logging.log4j.core.config.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.core.Configuration;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;

class MarkerFilterPropertiesCaseTest {

    @Test
    @LoggerContextSource("log4j2-properties-markerfilter-miscase.properties")
    void testMarkerFilterPropertyCase(final Configuration config) {
        assertNotNull(config);
        assertEquals(LifeCycle.State.STARTED, config.getState(), "Configuration did not start");
    }
}
