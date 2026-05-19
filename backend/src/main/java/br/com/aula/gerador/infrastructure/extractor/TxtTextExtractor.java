package br.com.aula.gerador.infrastructure.extractor;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class TxtTextExtractor implements TextExtractor {

    @Override
    public boolean supports(String extension) {
        return "txt".equalsIgnoreCase(extension);
    }

    @Override
    public String extract(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }
}
