package br.com.aula.gerador.application.service;

import java.util.Locale;

public final class FilenameNormalizer {

    private FilenameNormalizer() {
    }

    public static String normalize(String filename, String expectedExtension) {
        String safeName = (filename == null || filename.isBlank())
                ? "resposta_ia" + expectedExtension
                : filename;
        safeName = safeName.replaceAll("[^a-zA-Z0-9._-]", "_");

        String lower = safeName.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".docx") && !lower.endsWith(".txt")) {
            safeName = safeName + expectedExtension;
        }

        return safeName;
    }
}
