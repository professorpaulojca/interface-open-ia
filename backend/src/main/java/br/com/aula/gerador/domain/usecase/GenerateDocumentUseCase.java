package br.com.aula.gerador.domain.usecase;

import br.com.aula.gerador.domain.gateway.AttachmentTextExtractorGateway;
import br.com.aula.gerador.domain.gateway.GeneratedFileStorageGateway;
import br.com.aula.gerador.domain.gateway.OpenAiClientGateway;
import br.com.aula.gerador.domain.model.Attachment;
import br.com.aula.gerador.domain.model.DocumentType;
import br.com.aula.gerador.domain.model.ExtractedText;
import br.com.aula.gerador.domain.model.FileMetadata;
import br.com.aula.gerador.domain.model.GeneratedDocument;

public class GenerateDocumentUseCase {

    private final AttachmentTextExtractorGateway textExtractor;
    private final OpenAiClientGateway openAiClient;
    private final GeneratedFileStorageGateway storage;

    public GenerateDocumentUseCase(
            AttachmentTextExtractorGateway textExtractor,
            OpenAiClientGateway openAiClient,
            GeneratedFileStorageGateway storage
    ) {
        this.textExtractor = textExtractor;
        this.openAiClient = openAiClient;
        this.storage = storage;
    }

    public FileMetadata execute(String currentPrompt, Attachment attachment) {
        ExtractedText extracted = null;
        if (attachment != null) {
            extracted = textExtractor.extract(attachment);
        }

        DocumentType documentType = DocumentType.detectFromPrompt(currentPrompt);

        GeneratedDocument generated;
        if (documentType.usesCodeInterpreter()) {
            generated = openAiClient.generateWithCodeInterpreter(currentPrompt, documentType, extracted);
        } else {
            generated = openAiClient.generateSimpleDocument(currentPrompt, extracted);
        }

        return storage.save(generated);
    }
}
