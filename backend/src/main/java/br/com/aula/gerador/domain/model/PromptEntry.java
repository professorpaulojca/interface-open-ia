package br.com.aula.gerador.domain.model;

import java.time.LocalDateTime;

public record PromptEntry(
        LocalDateTime timestamp,
        String prompt,
        String generatedFilename,
        String downloadUrl
) {
}
