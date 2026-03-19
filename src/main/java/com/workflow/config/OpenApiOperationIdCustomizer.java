package com.workflow.config;

import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiOperationIdCustomizer {

    /**
     * Generates stable, unique operationIds by prefixing every method with its
     * controller name, e.g. AssetController.create → "assetCreate".
     *
     * This prevents the OpenAPI generator from appending numeric suffixes
     * (create1, create2…) when multiple controllers share the same Java method name.
     * No @Operation(operationId=…) annotations or Java method renames needed.
     */
    @Bean
    public OperationCustomizer operationIdCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            String controllerName = handlerMethod.getBeanType().getSimpleName();
            String prefix = controllerName.replace("Controller", "");
            String tagPrefix = Character.toLowerCase(prefix.charAt(0)) + prefix.substring(1);

            String methodName = handlerMethod.getMethod().getName();
            String capitalizedMethod = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);

            operation.setOperationId(tagPrefix + capitalizedMethod);
            return operation;
        };
    }
}
