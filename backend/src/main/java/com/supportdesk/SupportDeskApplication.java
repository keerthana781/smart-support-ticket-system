package com.supportdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SupportDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportDeskApplication.class, args);
    }
}
