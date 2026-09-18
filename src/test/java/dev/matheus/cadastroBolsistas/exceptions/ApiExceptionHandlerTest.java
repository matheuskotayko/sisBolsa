package dev.matheus.cadastroBolsistas.exceptions;

import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void emailDuplicadoRetorna409ComMensagemFixa() {
        ResponseEntity<ErroResponse> resposta =
                handler.emailDuplicado(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().mensagem()).isEqualTo("E-mail ja cadastrado.");
    }
}
