package br.com.aula.gerador.domain.exception;

public class RateLimitExceededException extends DomainException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
