package dev.matheus.cadastroBolsistas.dto;

import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/*
 * representacao publica de um usuario com identificadores publicos prefixados no estilo Stripe e vigencia da bolsa.
 */
@Schema(description = "Dados detalhados do usuário (Bolsista, Professor ou Administrador).")
public record UsuarioResponse(
        @Schema(description = "Identificador público único", example = "bol_k8s2M4n9P1q3W5z7A0b2")
        String id,

        @Schema(description = "Nome completo do usuário", example = "Lucas Oliveira")
        String nome,

        @Schema(description = "E-mail de acesso institucional", example = "lucas.oliveira@aluno.sisbolsa.com")
        String email,

        @Schema(description = "Tipo de perfil de acesso", example = "BOLSISTA", allowableValues = {"ADMIN", "PROFESSOR", "BOLSISTA"})
        String tipoUsuario,

        @Schema(description = "URL pública da foto de perfil", example = "https://images.unsplash.com/photo-1534528741775-53994a69daeb")
        String fotoUrl,

        @Schema(description = "Biografia ou apresentação acadêmica", example = "Estudante de Engenharia de Software e bolsista de IC.")
        String bio,

        @Schema(description = "Indica se o cadastro está ativo no sistema", example = "true")
        boolean ativo,

        @Schema(description = "Curso de graduação do bolsista", example = "Engenharia de Software")
        String curso,

        @Schema(description = "Matrícula acadêmica do bolsista", example = "20240101")
        String matricula,

        @Schema(description = "CPF formatado do bolsista", example = "123.456.789-00")
        String cpf,

        @Schema(description = "Telefone de contato", example = "(48) 99999-1234")
        String telefone,

        @Schema(description = "Data de nascimento", example = "2002-05-15")
        LocalDate dataNascimento,

        @Schema(description = "ID público do laboratório vinculado", example = "lab_a1B2c3D4e5F6g7H8i9J0")
        String laboratorioId,

        @Schema(description = "Nome do laboratório vinculado", example = "Laboratório de Sistemas Inteligentes (LSI)")
        String nomeLaboratorio,

        @Schema(description = "Cargo no laboratório", example = "DESENVOLVEDOR")
        String cargo,

        @Schema(description = "Modalidade da bolsa de pesquisa", example = "PIBIC", allowableValues = {"PIBIC", "PIBITI", "EXTENSAO", "MONITORIA", "INSTITUCIONAL", "OUTRO"})
        String modalidadeBolsa,

        @Schema(description = "Descrição formatada da modalidade de bolsa", example = "PIBIC - Iniciação Científica")
        String modalidadeBolsaDescricao,

        @Schema(description = "Valor mensal da bolsa em Reais (R$)", example = "700.00")
        Double valorBolsa,

        @Schema(description = "Data de início da vigência da bolsa", example = "2026-03-01")
        LocalDate dataInicioBolsa,

        @Schema(description = "Data de término da vigência da bolsa", example = "2026-12-31")
        LocalDate dataFimBolsa,

        @Schema(description = "Indica se a vigência da bolsa já expirou", example = "false")
        boolean bolsaVencida,

        @Schema(description = "Indica se a vigência da bolsa expira em menos de 30 dias", example = "false")
        boolean bolsaPrestesAVencer) {

    public static UsuarioResponse de(Usuario u) {
        if (u == null) {
            return null;
        }
        return new UsuarioResponse(
                u.getPublicId(), u.getNome(), u.getEmail(), u.getTipoUsuario(), u.getFotoUrl(), u.getBio(),
                u.isAtivo(), null, null, null, null, null, null, u.getNomeLaboratorio(), null,
                null, null, null, null, null, false, false);
    }

    public static UsuarioResponse de(Bolsista b) {
        if (b == null) {
            return null;
        }
        String modalidade = b.getModalidadeBolsa() != null ? b.getModalidadeBolsa().name() : null;
        String modalidadeDesc = b.getModalidadeBolsa() != null ? b.getModalidadeBolsa().getDescricao() : null;
        return new UsuarioResponse(
                b.getPublicId(), b.getNome(), b.getEmail(), b.getTipoUsuario(), b.getFotoUrl(), b.getBio(),
                b.isAtivo(), b.getCurso(), b.getMatricula(), b.getCpf(), b.getTelefone(),
                b.getDataNascimento(),
                b.getLaboratorioPublicId(),
                b.getNomeLaboratorio(),
                b.getCargo() != null ? b.getCargo().name() : null,
                modalidade,
                modalidadeDesc,
                b.getValorBolsa(),
                b.getDataInicioBolsa(),
                b.getDataFimBolsa(),
                b.isBolsaVencida(),
                b.isBolsaPrestesAVencer());
    }

    public static UsuarioResponse de(dev.matheus.cadastroBolsistas.model.Professor p) {
        if (p == null) {
            return null;
        }
        return new UsuarioResponse(
                p.getPublicId(), p.getNome(), p.getEmail(), p.getTipoUsuario(), p.getFotoUrl(), p.getBio(),
                p.isAtivo(), null, null, null, null, null, null, p.getNomeLaboratorio(), null,
                null, null, null, null, null, false, false);
    }
}
