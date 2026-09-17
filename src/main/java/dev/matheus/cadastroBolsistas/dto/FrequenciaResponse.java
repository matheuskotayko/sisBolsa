package dev.matheus.cadastroBolsistas.dto;

import dev.matheus.cadastroBolsistas.model.Frequencia;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Registro de frequência com horas apontadas e entregáveis.")
public record FrequenciaResponse(
        @Schema(description = "Identificador único da frequência (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "ID do bolsista responsável", example = "4fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID bolsistaId,

        @Schema(description = "Nome do bolsista", example = "Lucas Oliveira")
        String nomeBolsista,

        @Schema(description = "Data da realização da atividade", example = "2026-08-29")
        LocalDate data,

        @Schema(description = "Horas trabalhadas apontadas", example = "4.0")
        double horasTrabalhadas,

        @Schema(description = "Descrição detalhada das tarefas executadas", example = "Implementação dos testes unitários e refatoração do pipeline de dados.")
        String descricao,

        @Schema(description = "Link do entregável / pull request", example = "https://github.com/lab-lsi/nlp-medico/pull/42")
        String linkComprovante,

        @Schema(description = "Indica se o registro está ativo", example = "true")
        boolean ativo) {

    public static FrequenciaResponse de(Frequencia f) {
        if (f == null) {
            return null;
        }
        return new FrequenciaResponse(
                f.getId(), f.getBolsistaId(), f.getNomeBolsista(),
                f.getData(), f.getHorasTrabalhadas(), f.getDescricao(),
                f.getLinkComprovante(), f.isAtivo());
    }
}
