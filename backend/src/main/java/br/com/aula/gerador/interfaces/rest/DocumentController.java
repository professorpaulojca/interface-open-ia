package br.com.aula.gerador.interfaces.rest;

import br.com.aula.gerador.application.service.OperationLogger;
import br.com.aula.gerador.domain.exception.GeneratedFileNotFoundException;
import br.com.aula.gerador.domain.exception.PromptValidationException;
import br.com.aula.gerador.domain.gateway.GeneratedFileStorageGateway;
import br.com.aula.gerador.domain.model.Attachment;
import br.com.aula.gerador.domain.model.DownloadUrl;
import br.com.aula.gerador.domain.model.FileMetadata;
import br.com.aula.gerador.domain.model.FileSize;
import br.com.aula.gerador.domain.usecase.GenerateDocumentUseCase;
import br.com.aula.gerador.domain.usecase.ManagePromptHistoryUseCase;
import br.com.aula.gerador.domain.validation.AttachmentValidator;
import br.com.aula.gerador.interfaces.rest.dto.GenerateResponse;
import br.com.aula.gerador.interfaces.rest.mapper.AttachmentMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/documentos")
@Tag(name = "Documentos", description = "Geração de documentos e gerenciamento do histórico de prompts")
public class DocumentController {

    private final GenerateDocumentUseCase generateDocumentUseCase;
    private final ManagePromptHistoryUseCase managePromptHistoryUseCase;
    private final GeneratedFileStorageGateway fileStorageGateway;
    private final AttachmentValidator attachmentValidator;

    public DocumentController(
            GenerateDocumentUseCase generateDocumentUseCase,
            ManagePromptHistoryUseCase managePromptHistoryUseCase,
            GeneratedFileStorageGateway fileStorageGateway,
            @Value("${app.max-attachment-bytes:5242880}") long maxAttachmentBytes
    ) {
        this.generateDocumentUseCase = generateDocumentUseCase;
        this.managePromptHistoryUseCase = managePromptHistoryUseCase;
        this.fileStorageGateway = fileStorageGateway;
        this.attachmentValidator = new AttachmentValidator(FileSize.ofBytes(maxAttachmentBytes));
    }

    @PostMapping(value = "/gerar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    @Operation(summary = "Gerar documento", description = "Gera um arquivo e retorna um link para download. Aceita opcionalmente um arquivo anexo (pdf, docx, xlsx, txt) que será enviado junto com o prompt para a IA.")
    public GenerateResponse generate(
            @RequestParam("prompt") String prompt,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        if (prompt == null || prompt.isBlank()) {
            throw new PromptValidationException("O prompt é obrigatório.");
        }

        Attachment attachment = null;
        if (file != null && !file.isEmpty()) {
            attachment = AttachmentMapper.toDomain(file);
            attachment.validateAgainst(attachmentValidator);
            OperationLogger.logGenerationStarted("generate", prompt, java.util.Optional.of(attachment.size()));
        } else {
            OperationLogger.logGenerationStarted("generate", prompt, java.util.Optional.empty());
        }

        String fullHistory = managePromptHistoryUseCase.appendAndReadAll(prompt);
        OperationLogger.logHistoryLoaded(fullHistory.length());

        FileMetadata fileMetadata = generateDocumentUseCase.execute(prompt, attachment);
        OperationLogger.logDocumentGenerated(fileMetadata);

        DownloadUrl downloadUrl = DownloadUrl.forGeneratedFile(fileMetadata.id());
        managePromptHistoryUseCase.appendGeneratedFile(fileMetadata.filename(), downloadUrl);

        return new GenerateResponse(
                "Arquivo gerado com sucesso.",
                fileMetadata.id(),
                fileMetadata.filename(),
                fileMetadata.contentType(),
                fileMetadata.sizeInBytes(),
                downloadUrl.path()
        );
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "Baixar arquivo gerado", description = "Baixa um arquivo previamente gerado.")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        OperationLogger.logDownloadRequested(id);

        FileMetadata fileMetadata = fileStorageGateway.findById(id)
                .orElseThrow(() -> new GeneratedFileNotFoundException("Arquivo gerado não encontrado ou expirado."));

        byte[] bytes = fileStorageGateway.readContent(id);
        FileSize downloadedSize = FileSize.ofBytes(bytes.length);
        OperationLogger.logDownloadCompleted(fileMetadata, downloadedSize);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(fileMetadata.contentType()));
        headers.setContentLength(bytes.length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileMetadata.filename(), StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }

    @GetMapping("/historico")
    @Operation(summary = "Consultar histórico", description = "Retorna todo o histórico de prompts armazenado.")
    public Map<String, String> history() {
        return Map.of("historico", managePromptHistoryUseCase.readAll());
    }

    @DeleteMapping("/historico")
    @Operation(summary = "Apagar histórico", description = "Remove todo o histórico de prompts armazenado.")
    public Map<String, String> clearHistory() {
        managePromptHistoryUseCase.clear();
        return Map.of("mensagem", "Histórico de prompts apagado com sucesso.");
    }
}
