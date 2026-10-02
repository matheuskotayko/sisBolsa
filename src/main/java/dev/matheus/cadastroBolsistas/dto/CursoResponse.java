package dev.matheus.cadastroBolsistas.dto;

import dev.matheus.cadastroBolsistas.model.Curso;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Curso disponível para vínculo de bolsistas.")
public record CursoResponse(
        @Schema(description = "Identificador público único do curso", example = "cur_3f6i8j2l5m7s9v1w4x6y")
        String id,

        @Schema(description = "Nome do curso", example = "Ciência da Computação")
        String nome) {

    public static CursoResponse de(Curso c) {
        if (c == null) return null;
        return new CursoResponse(c.getPublicId(), c.getNome());
    }
}
