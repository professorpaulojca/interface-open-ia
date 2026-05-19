package br.com.aula.gerador.infrastructure.extractor;

import java.io.IOException;

public interface TextExtractor {

    boolean supports(String extension);

    String extract(byte[] content) throws IOException;
}
