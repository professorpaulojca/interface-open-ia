package br.com.aula.gerador.domain.model;

public record GeneratedDocument(
        String filename,
        String contentType,
        byte[] bytes
) {
}
