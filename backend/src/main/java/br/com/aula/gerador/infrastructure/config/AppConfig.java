package br.com.aula.gerador.infrastructure.config;

import br.com.aula.gerador.domain.gateway.AttachmentTextExtractorGateway;
import br.com.aula.gerador.domain.gateway.GeneratedFileStorageGateway;
import br.com.aula.gerador.domain.gateway.OpenAiClientGateway;
import br.com.aula.gerador.domain.gateway.PromptStorageGateway;
import br.com.aula.gerador.domain.usecase.GenerateDocumentUseCase;
import br.com.aula.gerador.domain.usecase.ManagePromptHistoryUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public GenerateDocumentUseCase generateDocumentUseCase(
            AttachmentTextExtractorGateway textExtractor,
            OpenAiClientGateway openAiClient,
            GeneratedFileStorageGateway storage
    ) {
        return new GenerateDocumentUseCase(textExtractor, openAiClient, storage);
    }

    @Bean
    public ManagePromptHistoryUseCase managePromptHistoryUseCase(PromptStorageGateway promptStorage) {
        return new ManagePromptHistoryUseCase(promptStorage);
    }
}
