package br.com.aula.gerador.domain.model;

public record FileMetadata(
        String id,
        String filename,
        String contentType,
        long sizeInBytes
) {
}
