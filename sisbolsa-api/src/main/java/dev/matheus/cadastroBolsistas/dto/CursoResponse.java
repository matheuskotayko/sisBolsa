package dev.matheus.cadastroBolsistas.dto;

import dev.matheus.cadastroBolsistas.model.Curso;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Curso disponível para vínculo de bolsistas.")
public record CursoResponse(
        @Schema(description = "Identificador único do curso (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Nome do curso", example = "Ciência da Computação")
        String nome) {

    public static CursoResponse de(Curso c) {
        return new CursoResponse(c.getId(), c.getNome());
    }
}
