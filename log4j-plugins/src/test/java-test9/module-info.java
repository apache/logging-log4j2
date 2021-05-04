module org.apache.logging.log4j.plugins.test {
    exports org.apache.logging.log4j.plugins.test.validation;

    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.plugins;

    provides org.apache.logging.log4j.plugins.processor.PluginService with org.apache.logging.log4j.plugins.test.validation.plugins.Log4jPlugins;
}