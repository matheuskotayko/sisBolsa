package dev.matheus.cadastroBolsistas.dto;

import dev.matheus.cadastroBolsistas.model.Administrador;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados detalhados do Administrador.")
public record AdministradorResponse(
        @Schema(description = "Identificador público único", example = "adm_8k1m3n7p0q2w4z6a9b1c")
        String id,

        @Schema(description = "Nome completo", example = "Carlos Administrador")
        String nome,

        @Schema(description = "E-mail de acesso institucional", example = "carlos.admin@sisbolsa.com")
        String email,

        @Schema(description = "Tipo de perfil", example = "ADMIN")
        String tipoUsuario,

        @Schema(description = "URL da foto de perfil", example = "https://images.unsplash.com/photo-1534528741775-53994a69daeb")
        String fotoUrl,

        @Schema(description = "Biografia", example = "Administrador Geral do SisBolsa")
        String bio,

        @Schema(description = "Cargo institucional", example = "Coordenador Geral de TI")
        String cargo,

        @Schema(description = "Telefone de contato", example = "(55) 99999-1234")
        String telefone,

        @Schema(description = "Indica se está ativo no sistema", example = "true")
        boolean ativo
) {
    public static AdministradorResponse de(Administrador a) {
        if (a == null) return null;
        return new AdministradorResponse(
                a.getPublicId(),
                a.getNome(),
                a.getEmail(),
                a.getTipoUsuario(),
                a.getFotoUrl(),
                a.getBio(),
                a.getCargo(),
                a.getTelefone(),
                a.isAtivo()
        );
    }
}
