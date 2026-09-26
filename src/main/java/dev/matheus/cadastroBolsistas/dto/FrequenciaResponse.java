package dev.matheus.cadastroBolsistas.dto;

import dev.matheus.cadastroBolsistas.model.Frequencia;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Registro de frequência com horas apontadas e entregáveis.")
public record FrequenciaResponse(
        @Schema(description = "Identificador público único da frequência", example = "frq_2e5h7i1k4l6r8u0v3w5x")
        String id,

        @Schema(description = "ID público do bolsista responsável", example = "bol_k8s2M4n9P1q3W5z7A0b2")
        String bolsistaId,

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
                f.getPublicId(), f.getBolsistaPublicId(), f.getNomeBolsista(),
                f.getData(), f.getHorasTrabalhadas(), f.getDescricao(),
                f.getLinkComprovante(), f.isAtivo());
    }
}
