package br.com.aula.gerador.domain.model;

import java.util.Objects;

public final class DownloadUrl {

    private final String path;
    private final String baseUrl;

    private DownloadUrl(String path, String baseUrl) {
        this.path = Objects.requireNonNull(path, "Path não pode ser nulo");
        this.baseUrl = baseUrl;
    }

    public static DownloadUrl fromPath(String path) {
        return new DownloadUrl(path, null);
    }

    public static DownloadUrl fromPathAndBase(String path, String baseUrl) {
        return new DownloadUrl(path, removeTrailingSlash(baseUrl));
    }

    public static DownloadUrl forGeneratedFile(String fileId) {
        return new DownloadUrl("/api/documentos/download/" + fileId, null);
    }

    public String toAbsoluteUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return path;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return baseUrl + normalizedPath;
    }

    public String path() {
        return path;
    }

    private static String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadUrl that = (DownloadUrl) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return toAbsoluteUrl();
    }
}
