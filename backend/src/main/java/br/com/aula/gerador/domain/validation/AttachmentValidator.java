package br.com.aula.gerador.domain.validation;

import br.com.aula.gerador.domain.exception.AttachmentValidationException;
import br.com.aula.gerador.domain.model.FileExtension;
import br.com.aula.gerador.domain.model.FileSize;

public final class AttachmentValidator {

    private final FileSize maxSize;

    public AttachmentValidator(FileSize maxSize) {
        this.maxSize = maxSize;
    }

    public void validate(String filename, FileSize size, FileExtension extension) {
        validateFilename(filename);
        validateNotEmpty(size);
        extension.validateSupported();
        size.validateNotExceeding(maxSize);
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new AttachmentValidationException("Nome do arquivo anexo é obrigatório.");
        }
    }

    private void validateNotEmpty(FileSize size) {
        if (size.toBytes() == 0) {
            throw new AttachmentValidationException("O arquivo anexo está vazio.");
        }
    }
}
