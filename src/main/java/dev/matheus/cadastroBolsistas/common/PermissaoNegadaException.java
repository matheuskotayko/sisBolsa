package dev.matheus.cadastroBolsistas.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PermissaoNegadaException extends ResponseStatusException {
    public PermissaoNegadaException(String mensagem) {
        super(HttpStatus.FORBIDDEN, mensagem);
    }
}
