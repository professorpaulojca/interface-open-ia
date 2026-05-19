package br.com.aula.gerador.domain.model;

import br.com.aula.gerador.domain.exception.AttachmentValidationException;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class FileExtension {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "xlsx", "txt");

    private final String value;

    private FileExtension(String value) {
        this.value = normalize(value);
    }

    public static FileExtension fromFilename(String filename) {
        if (filename == null) {
            throw new AttachmentValidationException("Nome do arquivo não pode ser nulo");
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new AttachmentValidationException(
                    "O arquivo anexo precisa ter uma extensão (pdf, docx, xlsx ou txt)."
            );
        }
        return new FileExtension(filename.substring(dot + 1));
    }

    public static FileExtension of(String extension) {
        return new FileExtension(extension);
    }

    private static String normalize(String extension) {
        if (extension == null) {
            return "";
        }
        return extension.toLowerCase(Locale.ROOT).trim();
    }

    public void validateSupported() {
        if (value.isEmpty()) {
            throw new AttachmentValidationException(
                    "O arquivo anexo precisa ter uma extensão (pdf, docx, xlsx ou txt)."
            );
        }
        if (!ALLOWED_EXTENSIONS.contains(value)) {
            throw new AttachmentValidationException(
                    String.format("Extensão não suportada: .%s. Permitidas: %s.",
                            value, String.join(", ", ALLOWED_EXTENSIONS))
            );
        }
    }

    public String value() {
        return value;
    }

    public boolean isSupported() {
        return ALLOWED_EXTENSIONS.contains(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileExtension that = (FileExtension) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "." + value;
    }
}
