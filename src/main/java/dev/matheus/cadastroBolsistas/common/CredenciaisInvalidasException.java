package dev.matheus.cadastroBolsistas.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CredenciaisInvalidasException extends ResponseStatusException {
    public CredenciaisInvalidasException(String mensagem) {
        super(HttpStatus.UNAUTHORIZED, mensagem);
    }
}
