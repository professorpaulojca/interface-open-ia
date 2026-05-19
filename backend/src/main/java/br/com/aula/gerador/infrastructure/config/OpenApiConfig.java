package br.com.aula.gerador.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gerador IA Code Interpreter API")
                        .version("1.0.0")
                        .description("API para gerar documentos DOCX ou XLSX usando OpenAI Code Interpreter."));
    }
}
