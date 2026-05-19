package br.com.aula.gerador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
public class GeradorIaApplication {
    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(GeradorIaApplication.class, args);
    }

    private static void loadDotEnv() {
        findDotEnv().ifPresent(path -> {
            try {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    applyDotEnvLine(line);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Não foi possível carregar o arquivo .env em " + path.toAbsolutePath(), e);
            }
        });
    }

    private static java.util.Optional<Path> findDotEnv() {
        List<Path> candidates = List.of(
                Path.of(".env"),
                Path.of("backend", ".env")
        );

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return java.util.Optional.of(candidate);
            }
        }

        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return java.util.Optional.of(candidate);
            }
            current = current.getParent();
        }

        return java.util.Optional.empty();
    }

    private static void applyDotEnvLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }

        int separatorIndex = trimmed.indexOf('=');
        if (separatorIndex <= 0) {
            return;
        }

        String key = trimmed.substring(0, separatorIndex).trim();
        String value = trimmed.substring(separatorIndex + 1).trim();

        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }

        if (System.getenv(key) == null && System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
