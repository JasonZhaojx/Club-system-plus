package com.backend.sever.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI clubSystemPlusOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Club System Plus API")
                        .description("社团官网与活动运营管理系统接口文档")
                        .version("v0.1.0")
                        .license(new License().name("Apache-2.0")))
                .servers(List.of(new Server().url("/api").description("本地开发接口前缀")));
    }
}
