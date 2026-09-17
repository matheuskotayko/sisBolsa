package dev.matheus.cadastroBolsistas.dto;

import dev.matheus.cadastroBolsistas.model.Projeto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Dados detalhados do projeto com métricas de membros e links.")
public record ProjetoResponse(
        @Schema(description = "Identificador único do projeto (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Título / Nome do projeto", example = "Processamento de Linguagem Natural para Documentos Médicos")
        String nome,

        @Schema(description = "Descrição detalhada do projeto", example = "Desenvolvimento de modelos LLM para sumarização de prontuários clínicos.")
        String descricao,

        @Schema(description = "ID do laboratório vinculado", example = "4fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID laboratorioId,

        @Schema(description = "Nome do laboratório", example = "Laboratório de Sistemas Inteligentes (LSI)")
        String nomeLaboratorio,

        @Schema(description = "Indica se o projeto está ativo", example = "true")
        boolean ativo,

        @Schema(description = "Total de bolsistas e pesquisadores membros do projeto", example = "4")
        int totalMembros,

        @Schema(description = "Link do repositório externo no GitHub/GitLab", example = "https://github.com/lab-lsi/nlp-medico")
        String linkRepositorio,

        @Schema(description = "Link da documentação ou artigo publicado", example = "https://www.overleaf.com/read/exemplo123")
        String linkDocumentacao) {

    public static ProjetoResponse de(Projeto p) {
        return de(p, 0);
    }

    public static ProjetoResponse de(Projeto p, int totalMembros) {
        if (p == null) {
            return null;
        }
        return new ProjetoResponse(
                p.getId(), p.getNome(), p.getDescricao(),
                p.getLaboratorioId(),
                p.getNomeLaboratorio(), p.isAtivo(), totalMembros,
                p.getLinkRepositorio(), p.getLinkDocumentacao());
    }
}
