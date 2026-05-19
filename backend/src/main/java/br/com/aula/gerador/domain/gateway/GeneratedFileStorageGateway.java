package br.com.aula.gerador.domain.gateway;

import br.com.aula.gerador.domain.model.FileMetadata;
import br.com.aula.gerador.domain.model.GeneratedDocument;

import java.util.Optional;

public interface GeneratedFileStorageGateway {

    FileMetadata save(GeneratedDocument document);

    Optional<FileMetadata> findById(String id);

    byte[] readContent(String id);
}
