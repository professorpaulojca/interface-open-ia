package br.com.aula.gerador.domain.model;

import br.com.aula.gerador.domain.exception.AttachmentValidationException;

import java.util.Objects;

public final class FileSize {

    private final long bytes;

    private FileSize(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Tamanho não pode ser negativo");
        }
        this.bytes = bytes;
    }

    public static FileSize ofBytes(long bytes) {
        return new FileSize(bytes);
    }

    public static FileSize ofKilobytes(long kb) {
        return new FileSize(kb * 1024);
    }

    public static FileSize ofMegabytes(long mb) {
        return new FileSize(mb * 1024 * 1024);
    }

    public long toBytes() {
        return bytes;
    }

    public long toKilobytes() {
        return bytes / 1024;
    }

    public long toMegabytes() {
        return bytes / (1024 * 1024);
    }

    public boolean exceeds(FileSize limit) {
        return this.bytes > limit.bytes;
    }

    public void validateNotExceeding(FileSize limit) {
        if (exceeds(limit)) {
            throw new AttachmentValidationException(
                    String.format("O arquivo anexado tem %d KB e excede o limite de %d KB para envio à OpenAI. " +
                            "Anexe um arquivo menor para evitar estouro do limite de tokens por minuto.",
                            this.toKilobytes(), limit.toKilobytes())
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileSize fileSize = (FileSize) o;
        return bytes == fileSize.bytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bytes);
    }

    @Override
    public String toString() {
        return bytes >= 1024 * 1024
                ? toMegabytes() + " MB"
                : bytes >= 1024
                ? toKilobytes() + " KB"
                : bytes + " bytes";
    }
}
