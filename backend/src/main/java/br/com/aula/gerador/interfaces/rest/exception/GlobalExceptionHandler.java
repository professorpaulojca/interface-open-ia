package br.com.aula.gerador.interfaces.rest.exception;

import br.com.aula.gerador.domain.exception.AttachmentValidationException;
import br.com.aula.gerador.domain.exception.ContextLengthExceededException;
import br.com.aula.gerador.domain.exception.DocumentGenerationException;
import br.com.aula.gerador.domain.exception.DomainException;
import br.com.aula.gerador.domain.exception.GeneratedFileNotFoundException;
import br.com.aula.gerador.domain.exception.PromptValidationException;
import br.com.aula.gerador.domain.exception.RateLimitExceededException;
import br.com.aula.gerador.interfaces.rest.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            AttachmentValidationException.class,
            PromptValidationException.class,
            RateLimitExceededException.class,
            ContextLengthExceededException.class
    })
    public ResponseEntity<ApiError> handleClientErrors(DomainException ex, HttpServletRequest request) {
        return handle(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(GeneratedFileNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(GeneratedFileNotFoundException ex, HttpServletRequest request) {
        return handle(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler({
            DocumentGenerationException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ApiError> handleServerErrors(RuntimeException ex, HttpServletRequest request) {
        return handle(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        return handle(ex, HttpStatus.INTERNAL_SERVER_ERROR, request, "Erro interno inesperado.");
    }

    private ResponseEntity<ApiError> handle(Throwable ex, HttpStatus status, HttpServletRequest request) {
        return handle(ex, status, request, ex.getMessage());
    }

    private ResponseEntity<ApiError> handle(Throwable ex, HttpStatus status, HttpServletRequest request, String message) {
        String context = ex.getClass().getSimpleName();
        String safeMessage = message == null ? "" : message;

        if (status.is5xxServerError()) {
            log.error("[{}] {}", context, safeMessage, ex);
        } else {
            log.warn("[{}] {}", context, safeMessage);
        }

        ApiError error = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), safeMessage, request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}
