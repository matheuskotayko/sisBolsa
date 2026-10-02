package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * atualizacao do proprio perfil. os tres campos de senha so importam juntos:
 * trocar a senha exige a atual, a nova e a confirmacao - fica manual no
 * controller porque e regra entre tres campos, bean validation nao cobre.
 */
@Schema(description = "Dados para atualização de perfil e alteração opcional de senha.")
public record PerfilRequest(
        @NotBlank(message = "O nome deve ter pelo menos 3 caracteres.")
        @Size(min = 3, message = "O nome deve ter pelo menos 3 caracteres.")
        @Schema(description = "Nome completo", example = "Admin", requiredMode = Schema.RequiredMode.REQUIRED)
        String nome,

        @NotBlank(message = "E-mail e obrigatorio.")
        @Email(message = "Informe um e-mail valido.")
        @Schema(description = "E-mail de acesso", example = "admin@sisbolsa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "URL pública da foto de perfil", example = "https://ui-avatars.com/api/?name=Admin", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String fotoUrl,

        @Schema(description = "Biografia ou resumo acadêmico", example = "Administrador do SisBolsa e responsável pela gestão das bolsas do CTISM.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String bio,

        @Schema(description = "Senha atual (obrigatória apenas se desejar trocar a senha)", example = "12345678", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String senhaAtual,

        @Schema(description = "Nova senha desejada (mínimo 6 caracteres)", example = "12345678", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String senha,

        @Schema(description = "Confirmação da nova senha", example = "12345678", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String confirmaSenha) {
}
