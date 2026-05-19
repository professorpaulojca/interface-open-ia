package br.com.aula.gerador.infrastructure.extractor;

import br.com.aula.gerador.domain.gateway.AttachmentTextExtractorGateway;
import br.com.aula.gerador.domain.model.Attachment;
import br.com.aula.gerador.domain.model.ExtractedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class AttachmentTextExtractorAdapter implements AttachmentTextExtractorGateway {

    private static final Logger log = LoggerFactory.getLogger(AttachmentTextExtractorAdapter.class);

    private final int maxChars;
    private final List<TextExtractor> extractors;

    public AttachmentTextExtractorAdapter(
            @Value("${app.max-attachment-chars:60000}") int maxChars,
            List<TextExtractor> extractors
    ) {
        this.maxChars = maxChars;
        this.extractors = extractors;
    }

    @Override
    public ExtractedText extract(Attachment attachment) {
        String rawText = extractRawText(attachment);

        String normalized = rawText == null ? "" : rawText.replace("\r\n", "\n").trim();
        boolean truncated = false;
        if (maxChars > 0 && normalized.length() > maxChars) {
            normalized = normalized.substring(0, maxChars);
            truncated = true;
        }

        log.info("Texto extraído do anexo {}. Caracteres: {} (truncado: {})",
                attachment.filename(), normalized.length(), truncated);

        return new ExtractedText(
                attachment.filename(),
                attachment.extension().value(),
                normalized,
                truncated,
                rawText == null ? 0 : rawText.length()
        );
    }

    private String extractRawText(Attachment attachment) {
        for (TextExtractor extractor : extractors) {
            if (extractor.supports(attachment.extension().value())) {
                try {
                    return extractor.extract(attachment.content());
                } catch (IOException e) {
                    log.error("Falha ao extrair texto do anexo: {}", attachment.filename(), e);
                    return "";
                }
            }
        }
        log.warn("Extensão não suportada para extração de texto: {}", attachment.extension());
        return "";
    }
}
