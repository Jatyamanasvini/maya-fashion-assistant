package com.maya.fashionassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FashionAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(FashionAssistantApplication.class, args);
    }
}
