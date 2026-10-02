package dev.matheus.cadastroBolsistas.exceptions;

/**
 * Exceção de domínio para falhas internas não esperadas (ex: erro ao gerar PDF).
 * Mapeada para HTTP 500 pelo {@link ApiExceptionHandler}.
 */
public class ErroInternoException extends RuntimeException {
    public ErroInternoException(String mensagem) {
        super(mensagem);
    }
}
