package timp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;

import jakarta.validation.Valid;
import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Management REST API")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Вставьте JWT access token, полученный при login/register/refresh")));
    }

    @Bean
    OperationCustomizer addBearerAuth() {
        return (operation, handlerMethod) -> {
            Class<?> controllerClass = handlerMethod.getBeanType();
            if (controllerClass == timp.controller.AuthController.class || controllerClass == timp.controller.SensorController.class) {
                return operation;
            }
            operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
            return operation;
        };
    }

    @Bean
    OperationCustomizer fixResponseCodes() {
        return (operation, handlerMethod) -> {
            var ct = handlerMethod.getBeanType();
            String methodName = handlerMethod.getMethod().getName();
            boolean hasPathVar = Arrays.stream(handlerMethod.getMethod().getParameters())
                    .anyMatch(p -> p.isAnnotationPresent(PathVariable.class));
            boolean hasValidBody = Arrays.stream(handlerMethod.getMethod().getParameters())
                    .anyMatch(p -> p.isAnnotationPresent(RequestBody.class) && p.isAnnotationPresent(Valid.class));
            boolean isDelete = handlerMethod.hasMethodAnnotation(DeleteMapping.class);
            boolean isPostCreate = handlerMethod.hasMethodAnnotation(PostMapping.class)
                    && methodName.startsWith("create");
            boolean isFireAccess = ct == timp.controller.FireAccessController.class;
            boolean isSensor = ct == timp.controller.SensorController.class;

            var responses = operation.getResponses();

            if (isDelete) {
                var resp200 = responses.remove("200");
                if (resp200 != null) {
                    responses.addApiResponse("204", resp200);
                }
            }

            if (isPostCreate) {
                var resp200 = responses.remove("200");
                if (resp200 != null) {
                    responses.addApiResponse("201", resp200);
                }
            }

            if (hasValidBody && !isFireAccess && responses.get("400") == null) {
                responses.addApiResponse("400",
                        new ApiResponse().description("Bad Request – ошибка валидации"));
            }

            if (hasPathVar && !isSensor && responses.get("404") == null) {
                responses.addApiResponse("404",
                        new ApiResponse().description("Not Found – ресурс не найден"));
            }
            if (isSensor && methodName.equals("receiveReading") && responses.get("404") == null) {
                responses.addApiResponse("404",
                        new ApiResponse().description("Not Found – датчик не найден"));
            }

            if (responses.get("500") == null) {
                responses.addApiResponse("500",
                        new ApiResponse().description("Internal Server Error – внутренняя ошибка сервера"));
            }

            return operation;
        };
    }
}
