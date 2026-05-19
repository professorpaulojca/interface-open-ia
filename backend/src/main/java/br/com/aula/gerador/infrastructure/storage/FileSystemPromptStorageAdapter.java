package br.com.aula.gerador.infrastructure.storage;

import br.com.aula.gerador.application.service.HistoryEntryFormatter;
import br.com.aula.gerador.domain.gateway.PromptStorageGateway;
import br.com.aula.gerador.domain.model.DownloadUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

@Component
public class FileSystemPromptStorageAdapter implements PromptStorageGateway {

    private final Path promptFile;
    private final String publicBaseUrl;

    public FileSystemPromptStorageAdapter(
            @Value("${app.prompt-file}") String promptFile,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.promptFile = Path.of(promptFile);
        this.publicBaseUrl = removeTrailingSlash(publicBaseUrl);
    }

    @Override
    public synchronized String appendAndReadAll(String prompt) {
        try {
            createParentDirectoryIfNecessary();
            String entry = HistoryEntryFormatter.formatPromptEntry(LocalDateTime.now(), prompt);

            Files.writeString(
                    promptFile,
                    entry,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            return Files.readString(promptFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível gravar ou ler o arquivo de prompts.", e);
        }
    }

    @Override
    public synchronized void appendGeneratedFile(String filename, DownloadUrl downloadUrl) {
        try {
            createParentDirectoryIfNecessary();
            DownloadUrl absoluteUrl = DownloadUrl.fromPathAndBase(downloadUrl.path(), publicBaseUrl);
            String entry = HistoryEntryFormatter.formatGeneratedFileEntry(filename, absoluteUrl);

            Files.writeString(
                    promptFile,
                    entry,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível gravar o link do arquivo gerado no histórico.", e);
        }
    }

    @Override
    public synchronized String readAll() {
        try {
            if (!Files.exists(promptFile)) {
                return "";
            }
            return Files.readString(promptFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o histórico de prompts.", e);
        }
    }

    @Override
    public synchronized void clear() {
        try {
            createParentDirectoryIfNecessary();
            Files.writeString(promptFile, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível limpar o histórico de prompts.", e);
        }
    }

    private void createParentDirectoryIfNecessary() throws IOException {
        Path parent = promptFile.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
