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
                        .description("API RESTful corporativa del sistema Almacén Warnes. " +
                                "Proporciona servicios integrales para:\n" +
                                "- **Módulo de Seguridad**: Autenticación, control de sesiones, gestión de usuarios, roles y sucursales.\n" +
                                "- **Módulo de Inventario**: Administración de stock físico, lotes con caducidad, acuerdos de proveedores, " +
                                "movimientos de almacén, auditorías y sala de espera.\n\n" +
                                "Desarrollada con Spring Boot, Spring Security y PostgreSQL.")
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

