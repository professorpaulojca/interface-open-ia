package br.com.aula.gerador.domain.usecase;

import br.com.aula.gerador.domain.gateway.PromptStorageGateway;
import br.com.aula.gerador.domain.model.DownloadUrl;

public class ManagePromptHistoryUseCase {

    private final PromptStorageGateway promptStorage;

    public ManagePromptHistoryUseCase(PromptStorageGateway promptStorage) {
        this.promptStorage = promptStorage;
    }

    public String appendAndReadAll(String prompt) {
        return promptStorage.appendAndReadAll(prompt);
    }

    public void appendGeneratedFile(String filename, DownloadUrl downloadUrl) {
        promptStorage.appendGeneratedFile(filename, downloadUrl);
    }

    public String readAll() {
        return promptStorage.readAll();
    }

    public void clear() {
        promptStorage.clear();
    }
}
