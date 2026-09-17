package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/*
 * bolsistaId so e respeitado para admin e professor. bolsista comum sempre
 * registra para si mesmo, independente do que mandar aqui.
 */
@Schema(description = "Dados para apontamento de horas de frequência e atividades realizadas.")
public record FrequenciaRequest(
        @Schema(description = "ID do bolsista (apenas para Admin e Professor; Bolsistas sempre registram para si mesmos)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID bolsistaId,

        @NotNull(message = "Data da frequencia e obrigatoria.")
        @Schema(description = "Data do apontamento de horas", example = "2026-08-29", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate data,

        @NotNull(message = "Horas trabalhadas precisam ser entre 0.5 e 24.")
        @DecimalMin(value = "0.5", message = "Horas trabalhadas precisam ser entre 0.5 e 24.")
        @DecimalMax(value = "24", message = "Horas trabalhadas precisam ser entre 0.5 e 24.")
        @Schema(description = "Quantidade de horas trabalhadas no dia (entre 0.5 e 24.0)", example = "4.0", requiredMode = Schema.RequiredMode.REQUIRED)
        Double horasTrabalhadas,

        @NotBlank(message = "Descricao da atividade e obrigatoria.")
        @Schema(description = "Descrição detalhada das atividades executadas", example = "Implementação dos testes unitários e refatoração do pipeline de dados.", requiredMode = Schema.RequiredMode.REQUIRED)
        String descricao,

        @Schema(description = "Link para o entregável ou pull request referente às atividades", example = "https://github.com/lab-lsi/nlp-medico/pull/42", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String linkComprovante) {
}
