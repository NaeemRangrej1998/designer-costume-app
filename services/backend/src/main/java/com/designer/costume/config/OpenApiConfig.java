package com.designer.costume.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI designerCostumeApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Designer Costume API")
                        .version("v1")
                        .description("Read-only catalog APIs and backend health."));
    }
}
