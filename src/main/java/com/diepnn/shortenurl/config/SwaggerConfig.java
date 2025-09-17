package com.diepnn.shortenurl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public ModelResolver modelResolverSnakeCase(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper.copy());
    }

}
