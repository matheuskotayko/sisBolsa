package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para redefinição de senha com código temporário.")
public record RedefinirSenhaRequest(
        @NotBlank(message = "E-mail e codigo de verificacao sao obrigatorios.")
        @Schema(description = "E-mail da conta", example = "admin@sisbolsa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @NotBlank(message = "E-mail e codigo de verificacao sao obrigatorios.")
        @Schema(description = "Código numérico de 6 dígitos recebido", example = "749201", requiredMode = Schema.RequiredMode.REQUIRED)
        String codigo,

        @NotBlank(message = "A nova senha deve ter pelo menos 6 caracteres.")
        @Size(min = 6, message = "A nova senha deve ter pelo menos 6 caracteres.")
        @Schema(description = "Nova senha (mínimo 6 caracteres)", example = "NovaSenha@2026", requiredMode = Schema.RequiredMode.REQUIRED)
        String novaSenha,

        @NotBlank(message = "A nova senha e a confirmacao nao conferem.")
        @Schema(description = "Confirmação da nova senha", example = "NovaSenha@2026", requiredMode = Schema.RequiredMode.REQUIRED)
        String confirmaSenha) {
}
