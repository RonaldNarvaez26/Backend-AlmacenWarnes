package com.almacen.warnes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Almacén Warnes - API REST")
                        .version("1.0.0")
                        .description("API RESTful corporativa para la administración y control de inventario físico, " +
                                "recepción y auditoría de lotes con caducidad, acuerdos logísticos con distribuidores, " +
                                "catálogo de productos y sala de espera. Desarrollada con Spring Boot y PostgreSQL.")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo Almacén Warnes")
                                .email("soporte@almacenwarnes.com"))
                        .license(new License()
                                .name("Uso Interno Empresarial")
                                .url("https://almacenwarnes.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor Local de Desarrollo")
                ));
    }
}

