package com.diepnn.shortenurl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public ModelResolver modelResolverSnakeCase(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper.copy());
    }

    @Bean
    GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                             .group("user")
                             .pathsToMatch("/api/**")
                             .pathsToExclude("/api/*/admin/**")
                             .build();
    }

    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                             .group("admin")
                             .pathsToMatch("/api/*/admin/**", "/api/*/auth/**")
                             .build();
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().components(new Components().addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()))
                            .info(new Info().title("My REST API")
                                            .description("API for shortening and retrieving long URLs.")
                                            .version("1.0")
                                            .contact(new Contact().name("Ageha")
                                                                  .email("agehachou2602@gmail.com")
                                                    )
                                 )
//                            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                ;
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                   .scheme("bearer")
                                   .bearerFormat("JWT");
    }
}
