package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo quantitativo global de entidades ativas no sistema.")
public record ResumoGeralResponse(
        @Schema(description = "Total de bolsistas ativos", example = "42")
        long totalBolsistas,
        @Schema(description = "Total de laboratórios ativos", example = "5")
        long totalLaboratorios,
        @Schema(description = "Total de projetos ativos", example = "12")
        long totalProjetos
) {
}
