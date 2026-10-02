package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo acumulado de horas trabalhadas de um bolsista.")
public record ResumoHorasResponse(
        @Schema(description = "Total de horas trabalhadas no mês atual", example = "32.5")
        Double horasMes,
        @Schema(description = "Total acumulado de horas trabalhadas em todo o período", example = "120.0")
        Double horasTotal
) {
}
