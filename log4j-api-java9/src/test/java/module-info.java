open module org.apache.logging.log4j.java9test {
    exports org.apache.logging.log4j.util.java9;
    requires org.apache.logging.log4j;
    requires transitive org.junit.jupiter.engine;
    requires transitive org.junit.jupiter.api;
}
