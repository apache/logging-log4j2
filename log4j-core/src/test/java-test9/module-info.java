module org.apache.logging.log4j.core.test {
    exports org.apache.logging.log4j.core.test;
    exports org.apache.logging.log4j.core.test.appender;
    exports org.apache.logging.log4j.core.test.hamcrest;
    exports org.apache.logging.log4j.core.test.junit;

    requires java.naming;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.test;
    requires org.apache.logging.log4j.plugins;
    requires org.apache.logging.log4j.plugins.test;
    requires org.apache.logging.log4j.core;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.engine;
    requires org.junit.jupiter.params;
    requires org.junit.platform.commons;
    requires org.junit.platform.engine;
}