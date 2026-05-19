package br.com.aula.gerador.application.service;

import java.util.Locale;

public final class ContentTypeResolver {

    private ContentTypeResolver() {
    }

    public static String resolve(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".txt")) {
            return "text/plain; charset=utf-8";
        }
        return "application/octet-stream";
    }
}
