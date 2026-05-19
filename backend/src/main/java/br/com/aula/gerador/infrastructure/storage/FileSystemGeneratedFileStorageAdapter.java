package br.com.aula.gerador.infrastructure.storage;

import br.com.aula.gerador.application.service.ContentTypeResolver;
import br.com.aula.gerador.domain.exception.GeneratedFileNotFoundException;
import br.com.aula.gerador.domain.gateway.GeneratedFileStorageGateway;
import br.com.aula.gerador.domain.model.FileMetadata;
import br.com.aula.gerador.domain.model.GeneratedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@Component
public class FileSystemGeneratedFileStorageAdapter implements GeneratedFileStorageGateway {

    private static final Logger log = LoggerFactory.getLogger(FileSystemGeneratedFileStorageAdapter.class);

    private final Path generatedFilesDir;

    public FileSystemGeneratedFileStorageAdapter(
            @Value("${app.generated-files-dir:data/generated}") String generatedFilesDir
    ) {
        this.generatedFilesDir = Path.of(generatedFilesDir);
    }

    @Override
    public FileMetadata save(GeneratedDocument document) {
        try {
            Files.createDirectories(generatedFilesDir);
            String id = UUID.randomUUID().toString();
            String sanitized = sanitizeFilename(document.filename());
            Path filePath = generatedFilesDir.resolve(id + "_" + sanitized).normalize();
            Files.write(filePath, document.bytes());
            log.info("Arquivo gerado salvo em disco. ID: {}, Path: {}, Bytes: {}", id, filePath.toAbsolutePath(), document.bytes().length);
            return new FileMetadata(id, sanitized, document.contentType(), document.bytes().length);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível salvar o arquivo gerado para download.", e);
        }
    }

    @Override
    public Optional<FileMetadata> findById(String id) {
        if (id == null || id.isBlank() || id.contains("..") || id.contains("/") || id.contains("\\")) {
            return Optional.empty();
        }

        try {
            if (!Files.isDirectory(generatedFilesDir)) {
                return Optional.empty();
            }

            try (var files = Files.list(generatedFilesDir)) {
                return files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith(id + "_"))
                        .findFirst()
                        .map(this::toFileMetadata);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível localizar o arquivo gerado.", e);
        }
    }

    @Override
    public byte[] readContent(String id) {
        Path path = findPathById(id)
                .orElseThrow(() -> new GeneratedFileNotFoundException("Arquivo gerado não encontrado ou expirado."));
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o arquivo gerado.", e);
        }
    }

    private Optional<Path> findPathById(String id) {
        if (id == null || id.isBlank() || id.contains("..") || id.contains("/") || id.contains("\\")) {
            return Optional.empty();
        }
        try {
            if (!Files.isDirectory(generatedFilesDir)) {
                return Optional.empty();
            }
            try (var files = Files.list(generatedFilesDir)) {
                return files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith(id + "_"))
                        .findFirst();
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private FileMetadata toFileMetadata(Path path) {
        try {
            String storedName = path.getFileName().toString();
            int separatorIndex = storedName.indexOf('_');
            String id = separatorIndex > 0 ? storedName.substring(0, separatorIndex) : storedName;
            String filename = separatorIndex > 0 ? storedName.substring(separatorIndex + 1) : storedName;
            return new FileMetadata(id, filename, ContentTypeResolver.resolve(filename), Files.size(path));
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler os metadados do arquivo gerado.", e);
        }
    }

    private String sanitizeFilename(String filename) {
        String safeName = filename == null || filename.isBlank() ? "resposta_ia.txt" : filename;
        return safeName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
