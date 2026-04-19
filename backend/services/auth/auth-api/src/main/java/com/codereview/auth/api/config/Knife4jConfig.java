package com.codereview.auth.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Knife4j Configuration for Auth Service
 *
 * API documentation configuration
 *
 * @author Auth Service Team
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("认证服务 API")
                        .version("1.0.0")
                        .description("用户认证与授权服务REST API文档")
                        .contact(new Contact()
                                .name("Platform Team")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("本地开发环境"),
                        new Server().url("https://auth.example.com").description("生产环境")));
    }
}
