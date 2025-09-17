package com.diepnn.shortenurl.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        JsonMapper mapper = JsonMapper.builder()
                                      .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                                      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                      .addModule(new JavaTimeModule())
                                      .build();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }
}
