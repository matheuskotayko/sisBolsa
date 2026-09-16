package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para cadastro de um novo curso.")
public record CursoRequest(
        @NotBlank(message = "Nome do curso e obrigatorio.")
        @Schema(description = "Nome do curso", example = "Engenharia de Software", requiredMode = Schema.RequiredMode.REQUIRED)
        String nome) {
}
