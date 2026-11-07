package dev.matheus.cadastroBolsistas.exceptions;

import dev.matheus.cadastroBolsistas.dto.response.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.response.ErroValidacaoCampo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/*
 * traduz excecao em json com uma unica forma: {"mensagem": "..."}.
 */
@Slf4j
@RestControllerAdvice(basePackages = "dev.matheus.cadastroBolsistas")
public class ApiExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErroResponse("Acesso negado."));
    }

    /* excecoes de dominio estendem ResponseStatusException: o status vem da propria excecao. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> statusException(ResponseStatusException e) {
        String motivo = e.getReason() != null ? e.getReason() : "Erro na requisicao.";
        return ResponseEntity.status(e.getStatusCode()).body(new ErroResponse(motivo));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> dadosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErroResponse(e.getMessage()));
    }

    /*
     * falha de @Valid nos dtos de request (bean validation). formato de lista
     * por campo, um item por constraint violada, e nao {"mensagem": "..."}
     * porque aqui pode ter mais de um campo invalido ao mesmo tempo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ErroValidacaoCampo>> dadosInvalidos(MethodArgumentNotValidException e) {
        List<ErroValidacaoCampo> erros = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErroValidacaoCampo(fe.getField(), mensagemDe(fe)))
                .toList();
        return ResponseEntity.badRequest().body(erros);
    }

    private static String mensagemDe(FieldError erro) {
        return erro.getDefaultMessage() != null ? erro.getDefaultMessage() : "Valor invalido.";
    }

    /*
     * violacao de constraint do banco sem checagem previa no service: email
     * ou cpf duplicado (curso.nome ja e validado antes do insert) ou id de fk
     * que nao existe (ex: laboratorioId inventado).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> violacaoDeIntegridade(DataIntegrityViolationException e) {
        String causa = e.getMostSpecificCause().getMessage();
        String mensagem;
        if (causa.contains("foreign key constraint")) {
            mensagem = "Um dos IDs informados (ex: laboratorio) nao existe.";
        } else if (causa.contains("cpf")) {
            mensagem = "Este CPF ja esta cadastrado.";
        } else {
            mensagem = "E-mail ja cadastrado.";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(mensagem));
    }

    /*
     * rede final: qualquer coisa nao prevista vira 500 sem vazar stacktrace
     * nem detalhe interno do banco para o cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> erroInesperado(Exception e) {
        log.error("Erro inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse("Erro interno ao processar a requisicao."));
    }
}
