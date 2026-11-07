package dev.matheus.cadastroBolsistas.administrador;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastro ou atualização de perfil de Administrador.")
public record AdministradorRequest(
        @Schema(description = "Nome completo do administrador", example = "Carlos Administrador")
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome não pode ter mais de 150 caracteres.")
        String nome,

        @Schema(description = "E-mail de acesso institucional", example = "carlos.admin@sisbolsa.com")
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @Schema(description = "Senha de acesso (mínimo 6 caracteres)", example = "senhaSegura123")
        @Size(min = 6, message = "A senha precisa ter ao menos 6 caracteres.")
        String senha,

        @Schema(description = "URL da foto de perfil", example = "https://images.unsplash.com/photo-1534528741775-53994a69daeb")
        String fotoUrl,

        @Schema(description = "Biografia do administrador", example = "Administrador Geral do SisBolsa")
        String bio,

        @Schema(description = "Cargo institucional", example = "Coordenador Geral de TI")
        String cargo,

        @Schema(description = "Telefone de contato", example = "(55) 99999-1234")
        String telefone
) {
    /** senha em branco = nao alterar; vira null para o @Size ignorar. */
    public AdministradorRequest {
        senha = (senha != null && !senha.isBlank()) ? senha.trim() : null;
    }
}
