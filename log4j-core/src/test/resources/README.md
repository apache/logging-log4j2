This directory is mainly for storing configuration files used in various unit tests. As such, there are a few things
provided for your convenience in writing and maintaining unit tests.

Test Plugins
------------

Included are various Log4j plugins that are only useful for writing tests. To enable these plugins for your test,
make sure to include `org.apache.logging.log4j.test` in the root `packages` attribute of the config file. For example:

    <Configuration packages="org.apache.logging.log4j.test">
      <Appenders>
        <List name="List"/>
      </Appenders>
      <Loggers>
        <Logger name="org.apache.logging.log4j" level="debug">
          <AppenderRef ref="List"/>
        </Logger>
      </Loggers>
    </Configuration>

Note that if you don't specify a layout for a ListAppender, your log messages will be stored in a list of LogEvents.
If you use a SerializedLayout, your log messages will be stored in a list of byte arrays. If you specify any other
type of layout, your log messages will be stored in a list of strings. For more details, check out the class
`org.apache.logging.log4j.test.appender.ListAppender`.

Specifying Configuration Files in JUnit Tests
---------------------------------------------

Added in JUnit 4.9, the concept of test fixtures (i.e., the `@Before`, `@After`, etc. methods) has been expanded into
the concept of test rules. A test rule is a reusable test fixture such as the `TemporaryFolder` JUnit rule for creating
temporary directories and files on a test-by-test (or suite) basis. To use a test rule, you need to use the
`@Rule` or `@ClassRule` annotation to get a method-level or class-level test fixture respectively. For instance,
suppose your test class uses the file named `MyTestConfig.xml` in this directory. Then you can use the following rule
in your test class:

    @Rule
    public InitialLoggerContext context = new InitialLoggerContext("MyTestConfig.xml");

    @Test
    public void testSomeAwesomeFeature() {
        final LoggerContext ctx = context.getContext();
        final Logger logger = ctx.getLogger("org.apache.logging.log4j.my.awesome.test.logger");
        final Configuration cfg = ctx.getConfiguration();
        final ListAppender app = (ListAppender) cfg.getAppenders().get("List");
        logger.warn("Test message");
        final List<LogEvent> events = app.getEvents();
        // etc.
    }

Using this rule will automatically create a new LoggerContext using the specified configuration file for you to
retrieve via the `getContext()` method shown above. After the method finishes (or if you use `@ClassRule` and make
the field `static`), the `LoggerContext` is automatically stopped. No longer do you need to set any system properties,
reset the `StatusLogger` configuration, and all that other fun boilerplate code.

If you have any questions about writing unit tests, feel free to send an email to the dev mailing list, or check out
the JUnit documentation over at junit.org.