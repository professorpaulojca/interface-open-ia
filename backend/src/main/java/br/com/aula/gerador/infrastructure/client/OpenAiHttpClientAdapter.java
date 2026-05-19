package br.com.aula.gerador.infrastructure.client;

import br.com.aula.gerador.application.service.ContentTypeResolver;
import br.com.aula.gerador.application.service.FilenameNormalizer;
import br.com.aula.gerador.application.service.OpenAiErrorMapper;
import br.com.aula.gerador.application.service.OpenAiPromptBuilder;
import br.com.aula.gerador.application.service.OpenAiResponseParser;
import br.com.aula.gerador.application.service.OperationLogger;

import static br.com.aula.gerador.application.service.OpenAiResponseField.*;
import br.com.aula.gerador.domain.exception.DocumentGenerationException;
import br.com.aula.gerador.domain.gateway.OpenAiClientGateway;
import br.com.aula.gerador.domain.model.DocumentType;
import br.com.aula.gerador.domain.model.ExtractedText;
import br.com.aula.gerador.domain.model.FileSize;
import br.com.aula.gerador.domain.model.GeneratedDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Component
public class OpenAiHttpClientAdapter implements OpenAiClientGateway {

    private static final String OPERATION_CODE_INTERPRETER = "code_interpreter";
    private static final String OPERATION_SIMPLE = "simple";

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final String memoryLimit;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiResponseParser responseParser;
    private final OpenAiErrorMapper errorMapper;

    public OpenAiHttpClientAdapter(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model,
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.code-interpreter-memory}") String memoryLimit,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = removeTrailingSlash(baseUrl);
        this.memoryLimit = memoryLimit;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.responseParser = new OpenAiResponseParser(objectMapper);
        this.errorMapper = new OpenAiErrorMapper(objectMapper);
    }

    @Override
    public GeneratedDocument generateSimpleDocument(String currentPrompt, ExtractedText extractedText) {
        validateApiKey();

        try {
            String responseBody = callSimpleResponse(currentPrompt, extractedText);
            saveLastResponseForDebug(responseBody);

            JsonNode responseJson = responseParser.parseJson(responseBody);
            String text = responseParser.extractOutputText(responseJson);

            return new GeneratedDocument(
                    "resposta_ia.txt",
                    "text/plain; charset=utf-8",
                    text.getBytes(StandardCharsets.UTF_8)
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new DocumentGenerationException("Falha ao processar a resposta da OpenAI.", e);
        }
    }

    @Override
    public GeneratedDocument generateWithCodeInterpreter(String currentPrompt, DocumentType documentType, ExtractedText extractedText) {
        validateApiKey();

        try {
            String responseBody = callCodeInterpreterResponse(currentPrompt, documentType, extractedText);
            saveLastResponseForDebug(responseBody);

            JsonNode responseJson = responseParser.parseJson(responseBody);
            List<OpenAiResponseParser.ContainerFileCitation> citations = responseParser.collectCitations(responseJson);
            OperationLogger.logCitationsFound(citations.size());

            String expectedExtension = documentType.extension();
            OpenAiResponseParser.ContainerFileCitation chosen = responseParser.chooseBestCitation(citations, expectedExtension)
                    .orElseThrow(() -> new DocumentGenerationException(
                            "A OpenAI respondeu, mas não retornou uma anotação de arquivo do tipo container_file_citation. "
                                    + "Verifique data/last-openai-response.json para depuração."
                    ));

            byte[] fileBytes = downloadContainerFile(chosen.containerId(), chosen.fileId());
            String filename = FilenameNormalizer.normalize(chosen.filename(), expectedExtension);
            String contentType = ContentTypeResolver.resolve(filename);

            return new GeneratedDocument(filename, contentType, fileBytes);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new DocumentGenerationException("Falha ao processar a resposta da OpenAI.", e);
        }
    }

    private String callCodeInterpreterResponse(String currentPrompt, DocumentType documentType, ExtractedText extractedText)
            throws IOException, InterruptedException {

        ObjectNode body = objectMapper.createObjectNode();
        body.put(MODEL.fieldName(), model);
        body.put(INSTRUCTIONS.fieldName(), OpenAiPromptBuilder.buildCodeInterpreterInstructions(documentType));
        body.set(INPUT.fieldName(), buildInput(currentPrompt, extractedText));
        body.put(TOOL_CHOICE.fieldName(), "required");

        ArrayNode tools = body.putArray(TOOLS.fieldName());
        ObjectNode codeInterpreter = tools.addObject();
        codeInterpreter.put(TYPE.fieldName(), "code_interpreter");
        ObjectNode container = codeInterpreter.putObject(CONTAINER.fieldName());
        container.put(TYPE.fieldName(), "auto");
        container.put(MEMORY_LIMIT.fieldName(), memoryLimit);

        String requestBody = objectMapper.writeValueAsString(body);
        OperationLogger.logOpenAiRequest(OPERATION_CODE_INTERPRETER, model, requestBody.length());

        HttpRequest request = buildPostRequest("/responses", requestBody, Duration.ofMinutes(5));
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int bodyLength = response.body() == null ? 0 : response.body().length();
        OperationLogger.logOpenAiResponse(OPERATION_CODE_INTERPRETER, response.statusCode(), bodyLength);

        return assertSuccess(response);
    }

    private String callSimpleResponse(String currentPrompt, ExtractedText extractedText)
            throws IOException, InterruptedException {

        ObjectNode body = objectMapper.createObjectNode();
        body.put(MODEL.fieldName(), model);
        body.put(INSTRUCTIONS.fieldName(), OpenAiPromptBuilder.buildSimpleInstructions());
        body.set(INPUT.fieldName(), buildInput(currentPrompt, extractedText));

        String requestBody = objectMapper.writeValueAsString(body);
        OperationLogger.logOpenAiRequest(OPERATION_SIMPLE, model, requestBody.length());

        HttpRequest request = buildPostRequest("/responses", requestBody, Duration.ofMinutes(2));
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int bodyLength = response.body() == null ? 0 : response.body().length();
        OperationLogger.logOpenAiResponse(OPERATION_SIMPLE, response.statusCode(), bodyLength);

        return assertSuccess(response);
    }

    private JsonNode buildInput(String currentPrompt, ExtractedText extractedText) {
        String payload = OpenAiPromptBuilder.buildUserPayload(currentPrompt, extractedText);
        ArrayNode input = objectMapper.createArrayNode();
        ObjectNode message = input.addObject();
        message.put(ROLE.fieldName(), "user");
        ArrayNode content = message.putArray(CONTENT.fieldName());
        ObjectNode textPart = content.addObject();
        textPart.put(TYPE.fieldName(), "input_text");
        textPart.put(TEXT.fieldName(), payload);
        return input;
    }

    private HttpRequest buildPostRequest(String path, String body, Duration timeout) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
    }

    private String assertSuccess(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw errorMapper.map(response.statusCode(), response.body());
        }
        return response.body();
    }

    private byte[] downloadContainerFile(String containerId, String fileId) throws IOException, InterruptedException {
        String url = baseUrl + "/containers/" + containerId + "/files/" + fileId + "/content";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String error = new String(response.body(), StandardCharsets.UTF_8);
            throw new DocumentGenerationException("Erro ao baixar arquivo do container. HTTP " + response.statusCode() + ": " + error);
        }

        FileSize downloadedSize = FileSize.ofBytes(response.body().length);
        OperationLogger.logContainerDownload(containerId, fileId, downloadedSize);

        return response.body();
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("A variável de ambiente OPENAI_API_KEY não foi configurada.");
        }
    }

    private String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private void saveLastResponseForDebug(String responseBody) {
        try {
            Path dataDir = Path.of("data");
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            Files.writeString(dataDir.resolve("last-openai-response.json"), responseBody, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
