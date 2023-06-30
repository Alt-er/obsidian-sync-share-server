package com.alter.obsyncshare.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    private final ObjectMapper objectMapper;

    @Autowired
    public JacksonConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void configureJackson() {
        TimeZone timeZone = TimeZone.getDefault();
        objectMapper.setTimeZone(timeZone);
    }
}
