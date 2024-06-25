<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!-- tag::declaration[] -->
<%@ taglib prefix="log4j" uri="http://logging.apache.org/log4j/tld/log" %>
<!-- end::declaration[] -->
<!-- tag::entry-exit[] -->
<log4j:entry p0="${param.who}"/>
<!-- end::entry-exit[] -->
<html>
<head>
    <title>Log4j taglib example</title>
</head>
<body>
<p>This page presents several examples of Log4j Taglib usage:</p>
<ul>
    <li>
        Simple messages:
        <!-- tag::simple[] -->
        <log4j:debug message="Simple message"/>
        <!-- end::simple[] -->
    </li>
    <li>
        Parameterized messages:
        <!-- tag::simple[] -->
        <log4j:info message="Hello {}!" p0="${param.who}"/>
        <!-- end::simple[] -->
    </li>
    <li>
        Markers:
        <!-- tag::simple[] -->
        <log4j:warn message="Message with marker" marker="${requestScope.marker}"/>
        <!-- end::simple[] -->
    </li>
    <li>
        Catching/throwing:
        <!-- tag::catching[] -->
        <c:catch var="exception">
            <%= 5 / 0 %>
        </c:catch>
        <c:if test="${exception != null}">
            <log4j:catching exception="${exception}"/>
        </c:if>
        <!-- end::catching[] -->
    </li>
    <li>
        Testing for the current log level:
        <!-- tag::if-enabled[] -->
        <log4j:ifEnabled level="INFO">
            <code>INFO</code> is enabled.
        </log4j:ifEnabled>
        <!-- end::if-enabled[] -->
    </li>
    <li>
        Setting the logger name:
        <!-- tag::set-logger[] -->
        <log4j:setLogger logger="example.jsp"/>
        <!-- end::set-logger[] -->
    </li>
    <li>
        Dumping JSP scopes as HTML:
        <!-- tag::dump[] -->
        <log4j:dump scope="request"/>
        <!-- end::dump[] -->
    </li>
</ul>

<log4j:dump scope="request"/>

</body>
</html>
<!-- tag::entry-exit[] -->
<log4j:exit/>
<!-- end::entry-exit[] -->