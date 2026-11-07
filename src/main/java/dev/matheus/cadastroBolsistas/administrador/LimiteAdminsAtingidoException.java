package dev.matheus.cadastroBolsistas.administrador;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LimiteAdminsAtingidoException extends ResponseStatusException {
    public LimiteAdminsAtingidoException(String mensagem) {
        super(HttpStatus.CONFLICT, mensagem);
    }
}
