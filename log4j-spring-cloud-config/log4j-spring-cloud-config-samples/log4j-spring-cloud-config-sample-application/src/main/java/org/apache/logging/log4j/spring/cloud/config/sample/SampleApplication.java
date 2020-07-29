/*
 * Copyright (c) 2019 Nextiva, Inc. to Present.
 * All rights reserved.
 */
package org.apache.logging.log4j.spring.cloud.config.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SampleApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
            SpringApplication.run(SampleApplication.class, args);
    }

}
