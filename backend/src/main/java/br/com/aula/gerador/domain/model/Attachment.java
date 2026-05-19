package br.com.aula.gerador.domain.model;

import br.com.aula.gerador.domain.exception.AttachmentValidationException;
import br.com.aula.gerador.domain.validation.AttachmentValidator;

public record Attachment(
        String filename,
        FileExtension extension,
        byte[] content,
        String contentType,
        FileSize size
) {
    public Attachment {
        if (filename == null || filename.isBlank()) {
            throw new AttachmentValidationException("Nome do arquivo anexo é obrigatório.");
        }
        if (content == null || content.length == 0) {
            throw new AttachmentValidationException("O arquivo anexo está vazio.");
        }
        if (extension == null) {
            throw new AttachmentValidationException("Extensão do arquivo é obrigatória.");
        }
        if (size == null || size.toBytes() == 0) {
            throw new AttachmentValidationException("O arquivo anexo está vazio.");
        }
    }

    public static Attachment create(String filename, byte[] content, String contentType) {
        FileExtension extension = FileExtension.fromFilename(filename);
        FileSize size = FileSize.ofBytes(content.length);
        return new Attachment(filename, extension, content, contentType, size);
    }

    public void validateAgainst(AttachmentValidator validator) {
        validator.validate(filename, size, extension);
    }
}
