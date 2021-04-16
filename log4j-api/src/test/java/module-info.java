open module org.apache.logging.log4j {
    exports org.apache.logging.log4j.test.junit;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.assertj.core;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.engine;
    requires org.junit.jupiter.params;
    requires org.junit.platform.commons;
    requires org.junit.platform.engine;
    requires junit;

    uses org.apache.logging.log4j.spi.Provider;
    provides org.apache.logging.log4j.spi.Provider with org.apache.logging.log4j.TestProvider;
    uses org.apache.logging.log4j.util.PropertySource;
    uses org.apache.logging.log4j.message.ThreadDumpMessage.ThreadInfoFactory;
}