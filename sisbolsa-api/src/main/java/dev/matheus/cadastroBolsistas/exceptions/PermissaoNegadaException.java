package dev.matheus.cadastroBolsistas.exceptions;

public class PermissaoNegadaException extends RuntimeException {
    public PermissaoNegadaException(String mensagem) {
        super(mensagem);
    }
}
