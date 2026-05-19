package br.com.aula.gerador.application.service;

import br.com.aula.gerador.domain.exception.ContextLengthExceededException;
import br.com.aula.gerador.domain.exception.DocumentGenerationException;
import br.com.aula.gerador.domain.exception.RateLimitExceededException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class OpenAiErrorMapper {

    private final ObjectMapper objectMapper;

    public OpenAiErrorMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RuntimeException map(int statusCode, String body) {
        String safeBody = body == null ? "" : body;
        String code = "";
        String message = "";
        try {
            JsonNode node = objectMapper.readTree(safeBody);
            JsonNode error = node.path("error");
            code = error.path("code").asText("");
            message = error.path("message").asText("");
        } catch (IOException ignored) {
        }

        if (statusCode == 429 || "rate_limit_exceeded".equals(code) || "tokens".equalsIgnoreCase(extractErrorType(safeBody))) {
            String detalhe = message.isBlank() ? "limite de tokens por minuto excedido" : message;
            return new RateLimitExceededException(
                    "O prompt e/ou o arquivo anexado excedem o limite de tokens por minuto da OpenAI. "
                            + "Reduza o tamanho do prompt ou anexe um arquivo menor e tente novamente em alguns segundos. "
                            + "Detalhe da OpenAI: " + detalhe
            );
        }

        if (statusCode == 413 || "context_length_exceeded".equals(code)) {
            return new ContextLengthExceededException(
                    "O conteúdo enviado é maior do que o modelo suporta. Reduza o prompt ou o arquivo anexado e tente novamente."
            );
        }

        return new DocumentGenerationException("Erro na OpenAI. HTTP " + statusCode + ": " + safeBody);
    }

    private String extractErrorType(String body) {
        try {
            return objectMapper.readTree(body).path("error").path("type").asText("");
        } catch (IOException e) {
            return "";
        }
    }
}
