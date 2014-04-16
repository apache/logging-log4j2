package org.apache.logging.log4j.camel;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;

/**
 *
 */
public class LogEndpointTest extends ContextTestSupport {

    private static Exchange logged;

    private static class MyLogger extends LogProcessor {
        @Override
        public void process(final Exchange exchange) throws Exception {
            super.process(exchange);
            logged = exchange;
        }

        @Override
        public String toString() {
            return "myLogger";
        }
    }

    public void testLogEndpoint() throws Exception {
        final MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        template.sendBody("direct:start", "Hello World");
        assertMockEndpointsSatisfied();
        assertNotNull(logged);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final LogEndpoint endpoint = new LogEndpoint();
                endpoint.setCamelContext(context);
                endpoint.setLogger(new MyLogger());
                assertEquals("log4j:myLogger", endpoint.getEndpointUri());
                from("direct:start").to(endpoint).to("mock:result");
            }
        };
    }
}
