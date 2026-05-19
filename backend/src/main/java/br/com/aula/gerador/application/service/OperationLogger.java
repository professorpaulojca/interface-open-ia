package br.com.aula.gerador.application.service;

import br.com.aula.gerador.domain.model.FileMetadata;
import br.com.aula.gerador.domain.model.FileSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Optional;

public final class OperationLogger {

    private static final Logger log = LoggerFactory.getLogger(OperationLogger.class);

    private static final String MDC_OPERATION = "operation";
    private static final String MDC_FILE_ID = "fileId";
    private static final String MDC_PROMPT_LENGTH = "promptLength";

    private OperationLogger() {
    }

    public static void logGenerationStarted(String operation, String prompt, Optional<FileSize> attachmentSize) {
        try (var ignored = MDC.putCloseable(MDC_OPERATION, operation)) {
            if (attachmentSize.isPresent()) {
                log.info("Document generation started | promptLength={} | attachmentSize={}",
                        prompt.length(), attachmentSize.get().toKilobytes());
            } else {
                log.info("Document generation started | promptLength={} | noAttachment",
                        prompt.length());
            }
        }
    }

    public static void logHistoryLoaded(int totalLength) {
        log.debug("Prompt history loaded | totalChars={}", totalLength);
    }

    public static void logDocumentGenerated(FileMetadata metadata) {
        try (var ignored = MDC.putCloseable(MDC_FILE_ID, metadata.id())) {
            log.info("Document generated | filename={} | contentType={} | size={}",
                    metadata.filename(), metadata.contentType(), FileSize.ofBytes(metadata.sizeInBytes()));
        }
    }

    public static void logDownloadRequested(String fileId) {
        try (var ignored = MDC.putCloseable(MDC_FILE_ID, fileId)) {
            log.info("Download requested | fileId={}", fileId);
        }
    }

    public static void logDownloadCompleted(FileMetadata metadata, FileSize downloadedSize) {
        try (var ignored = MDC.putCloseable(MDC_FILE_ID, metadata.id())) {
            log.info("Download completed | filename={} | size={}", metadata.filename(), downloadedSize);
        }
    }

    public static void logOpenAiRequest(String operation, String model, int payloadSize) {
        log.debug("OpenAI request | operation={} | model={} | payloadChars={}", operation, model, payloadSize);
    }

    public static void logOpenAiResponse(String operation, int statusCode, int bodySize) {
        log.debug("OpenAI response | operation={} | status={} | bodyChars={}", operation, statusCode, bodySize);
    }

    public static void logCitationsFound(int count) {
        log.debug("File citations found | count={}", count);
    }

    public static void logContainerDownload(String containerId, String fileId, FileSize size) {
        log.debug("Container file downloaded | containerId={} | fileId={} | size={}",
                containerId, fileId, size);
    }

    public static void withContext(Map<String, String> context, Runnable operation) {
        context.forEach(MDC::put);
        try {
            operation.run();
        } finally {
            context.keySet().forEach(MDC::remove);
        }
    }
}
