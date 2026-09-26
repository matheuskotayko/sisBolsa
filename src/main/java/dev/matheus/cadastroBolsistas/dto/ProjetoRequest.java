package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para criação ou edição de um projeto de pesquisa.")
public record ProjetoRequest(
        @NotBlank(message = "Nome do projeto e obrigatorio.")
        @Schema(description = "Título / Nome do projeto de pesquisa", example = "Processamento de Linguagem Natural para Documentos Médicos", requiredMode = Schema.RequiredMode.REQUIRED)
        String nome,

        @NotBlank(message = "Descricao do projeto e obrigatoria.")
        @Schema(description = "Descrição dos objetivos e escopo do projeto", example = "Desenvolvimento de modelos LLM para sumarização de prontuários clínicos.", requiredMode = Schema.RequiredMode.REQUIRED)
        String descricao,

        @NotNull(message = "Projeto precisa estar vinculado a um laboratorio.")
        @Schema(description = "ID público do laboratório ao qual o projeto pertence", example = "lab_5h8k0l4n7o9u1x3y6z8a", requiredMode = Schema.RequiredMode.REQUIRED)
        String laboratorioId,

        @Schema(description = "Link para o repositório externo (ex: GitHub, GitLab)", example = "https://github.com/lab-lsi/nlp-medico", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String linkRepositorio,

        @Schema(description = "Link para documentação ou artigo (ex: Overleaf, Docs)", example = "https://www.overleaf.com/read/exemplo123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String linkDocumentacao) {
}
