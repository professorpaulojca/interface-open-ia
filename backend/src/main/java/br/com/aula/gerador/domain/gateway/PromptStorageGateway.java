package br.com.aula.gerador.domain.gateway;

import br.com.aula.gerador.domain.model.DownloadUrl;

public interface PromptStorageGateway {

    String appendAndReadAll(String prompt);

    void appendGeneratedFile(String filename, DownloadUrl downloadUrl);

    String readAll();

    void clear();
}
