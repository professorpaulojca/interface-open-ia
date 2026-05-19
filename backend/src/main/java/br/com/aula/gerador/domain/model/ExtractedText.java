package br.com.aula.gerador.domain.model;

public record ExtractedText(
        String filename,
        String extension,
        String text,
        boolean truncated,
        int originalLength
) {
}
