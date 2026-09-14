package dev.matheus.cadastroBolsistas.api;

import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.ErroValidacaoCampo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/*
 * traduz excecao em json com uma unica forma: {"mensagem": "..."}.
 * limitado ao pacote api para nao mexer no tratamento de erro das jsp.
 */
@RestControllerAdvice(basePackages = "dev.matheus.cadastroBolsistas.api")
public class ApiExceptionHandler {

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
     * rede final: qualquer coisa nao prevista vira 500 sem vazar stacktrace
     * nem detalhe interno do banco para o cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> erroInesperado(Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse("Erro interno ao processar a requisicao."));
    }
}
