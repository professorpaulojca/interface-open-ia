package br.com.aula.gerador.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateRequest(
        @NotBlank(message = "O prompt é obrigatório.")
        String prompt
) {
}
