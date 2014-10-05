This directory is mainly for storing configuration files used in various unit tests. As such, there are a few things
provided for your convenience in writing and maintaining unit tests.

Running Unit Tests
------------------

First, make sure you've recently run `mvn clean install` in `log4j-api` to get an up to date dependency. Then you can
run `mvn test` to run unit tests, or run `mvn verify` to run both unit tests and integration/performance tests.

Test Plugins
------------

Included are various Log4j plugins that are only useful for writing tests. These test plugins are automatically made
available to test classes. For instance, to use the ListAppender:

    <Configuration>
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
    public InitialLoggerContext init = new InitialLoggerContext("MyTestConfig.xml");

    @Test
    public void testSomeAwesomeFeature() {
        final LoggerContext ctx = init.getContext();
        final Logger logger = init.getLogger("org.apache.logging.log4j.my.awesome.test.logger");
        final Configuration cfg = init.getConfiguration();
        final ListAppender app = init.getListAppender("List");
        logger.warn("Test message");
        final List<LogEvent> events = app.getEvents();
        // etc.
    }

Using this rule will automatically create a new LoggerContext using the specified configuration file for you to
retrieve via the `getContext()` method shown above. After the method finishes (or if you use `@ClassRule` and make
the field `static`), the `LoggerContext` is automatically stopped. No longer do you need to set any system properties,
reset the `StatusLogger` configuration, and all that other fun boilerplate code.

Cleaning Up Test Log Files
--------------------------

The `CleanFiles` rule is also available to automatically delete a list of files after every test.

    @Rule
    public CleanFiles files = new CleanFiles("target/file1.log", "target/file2.log", "more files");

You can specify either a list of strings or a list of `File`s.

If you have any questions about writing unit tests, feel free to send an email to the dev mailing list, or check out
the JUnit documentation over at junit.org.

Specifying Test Categories
--------------------------

If your test is something more than just a unit test, it's usually a good idea to add a JUnit category to it. This
can be done at the class or method level:

    @Category(PerformanceTests.class)
    @Test
    public void testRandomAccessFileLogging() {
        // ...
    }

Various pre-defined categories are defined in `org.apache.logging.log4j.categories` in `log4j-core` test.
If you only want to run your test as part of the `maven-failsafe-plugin` integration tests phase, then simply name
your test `FooIT` instead of `FooTest` for automatic configuration.
