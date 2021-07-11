open module org.apache.logging.log4j.plugins {
    exports org.apache.logging.log4j.plugins;
    exports org.apache.logging.log4j.plugins.convert;
    exports org.apache.logging.log4j.plugins.di;
    exports org.apache.logging.log4j.plugins.di.spi;
    exports org.apache.logging.log4j.plugins.name;
    exports org.apache.logging.log4j.plugins.processor;
    exports org.apache.logging.log4j.plugins.util;
    exports org.apache.logging.log4j.plugins.bind;
    exports org.apache.logging.log4j.plugins.inject;

    exports org.apache.logging.log4j.plugins.validation;
    exports org.apache.logging.log4j.plugins.validation.constraints;
    exports org.apache.logging.log4j.plugins.validation.validators;
    exports org.apache.logging.log4j.plugins.test.validation;

    requires transitive java.compiler;
    requires transitive org.apache.logging.log4j;
    requires org.apache.logging.log4j.test;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.engine;
    requires org.junit.platform.commons;
    requires org.junit.platform.engine;
    requires junit;

    provides org.apache.logging.log4j.plugins.processor.PluginService with org.apache.logging.log4j.plugins.convert.plugins.Log4jPlugins;
    provides org.apache.logging.log4j.plugins.di.spi.BeanInfoService with org.apache.logging.log4j.plugins.convert.plugins.Log4jBeanInfo;
    provides javax.annotation.processing.Processor with org.apache.logging.log4j.plugins.processor.PluginProcessor, org.apache.logging.log4j.plugins.processor.BeanProcessor;

    uses org.apache.logging.log4j.plugins.processor.PluginService;
    uses org.apache.logging.log4j.plugins.di.spi.BeanInfoService;
}
