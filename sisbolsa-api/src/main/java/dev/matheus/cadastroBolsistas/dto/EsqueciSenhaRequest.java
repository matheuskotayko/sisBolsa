package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitação de código de recuperação de senha.")
public record EsqueciSenhaRequest(
        @NotBlank(message = "Informe um e-mail valido.")
        @Email(message = "Informe um e-mail valido.")
        @Schema(description = "E-mail cadastrado na conta", example = "admin@sisbolsa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email) {
}
