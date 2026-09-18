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
    void emailDuplicadoRetorna409ComMensagemDeEmail() {
        ResponseEntity<ErroResponse> resposta =
                handler.violacaoDeIntegridade(new DataIntegrityViolationException("duplicate key value violates unique constraint \"usuario_email_key\""));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().mensagem()).isEqualTo("E-mail ja cadastrado.");
    }

    @Test
    void fkInexistenteRetorna409ComMensagemDeId() {
        ResponseEntity<ErroResponse> resposta =
                handler.violacaoDeIntegridade(new DataIntegrityViolationException(
                        "insert or update on table \"projeto\" violates foreign key constraint \"projeto_laboratorio_id_fkey\""));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().mensagem()).isEqualTo("Um dos IDs informados (ex: laboratorio) nao existe.");
    }
}
