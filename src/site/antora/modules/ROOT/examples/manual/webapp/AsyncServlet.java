/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example;

import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.web.Log4jWebSupport;
import org.apache.logging.log4j.web.WebLoggerContextUtils;

@WebServlet(urlPatterns = "/async/*", asyncSupported = true)
public class AsyncServlet extends HttpServlet {

    private final Logger logger = LogManager.getLogger();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // tag::manual[]
        AsyncContext asyncContext = req.startAsync();
        Log4jWebSupport webSupport = WebLoggerContextUtils.getWebLifeCycle(getServletContext());
        asyncContext.start(() -> {
            try {
                webSupport.setLoggerContext();
                // Put your logic here
            } finally {
                webSupport.clearLoggerContext();
            }
        });
        // end::manual[]
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        // tag::automatic[]
        AsyncContext asyncContext = req.startAsync();
        asyncContext.start(WebLoggerContextUtils.wrapExecutionContext(getServletContext(), () -> {
            // Put your logic here
        }));
        // end::automatic[]
    }
}
