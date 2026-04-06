package com.platform.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Online Course Platform API",
                version = "1.0",
                description = """
            Test asosidagi online ta'lim platformasi.
            
            Qoidalar:
            • 1-dars bepul — hamma ko'ra oladi
            • Keyingi darsni ko'rish uchun — oldingi dars testini 80%+ topish kerak
            • Bo'lim tugagach — final testni topshirish kerak (80%+)
            • Final test o'tilsa — keyingi bo'lim ochiladi
            """,
                contact = @Contact(name = "Admin", email = "admin@platform.com")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local server")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Login/Register dan olingan JWT tokenni kiriting: Bearer {token}",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class SwaggerConfig {}