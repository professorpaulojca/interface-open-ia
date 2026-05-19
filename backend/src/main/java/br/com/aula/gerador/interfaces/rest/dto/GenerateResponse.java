package br.com.aula.gerador.interfaces.rest.dto;

public record GenerateResponse(
        String mensagem,
        String id,
        String filename,
        String contentType,
        long size,
        String downloadUrl
) {
}
