open module org.apache.logging.log4j.plugins {
    exports org.apache.logging.log4j.plugins;
    exports org.apache.logging.log4j.plugins.test.validation;

    requires java.compiler;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.test;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.engine;
    requires org.junit.platform.commons;
    requires org.junit.platform.engine;
    requires junit;

    provides org.apache.logging.log4j.plugins.processor.PluginService with org.apache.logging.log4j.plugins.convert.plugins.Log4jPlugins;
    provides javax.annotation.processing.Processor with org.apache.logging.log4j.plugins.processor.PluginProcessor;

    uses org.apache.logging.log4j.plugins.processor.PluginService;
}