package com.workflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI workFlowAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Work Flow API")
                        .description("This is the REST API for Work Flow")
                        .version("V0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token")
                        )
                )
                .tags(List.of(
                        new Tag().name("Authentication").description("User authentication and authorization"),
                        new Tag().name("Company").description("Company profile and settings"),
                        new Tag().name("Workers").description("Worker management"),
                        new Tag().name("Clients").description("Client management"),
                        new Tag().name("Job Templates").description("Job template definitions"),
                        new Tag().name("Jobs").description("Job instances"),
                        new Tag().name("Workflows").description("Workflow templates"),
                        new Tag().name("Job Workflows").description("Workflow instances for jobs"),
                        new Tag().name("Workflow Step Activities").description("Activity logs for workflow steps"),
                        new Tag().name("Assets").description("Asset management"),
                        new Tag().name("Asset Assignments").description("Asset job assignments")
                ));
    }
}
