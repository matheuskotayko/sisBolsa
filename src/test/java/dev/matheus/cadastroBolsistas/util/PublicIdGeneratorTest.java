package dev.matheus.cadastroBolsistas.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do gerador de Public IDs")
class PublicIdGeneratorTest {

    @Test
    @DisplayName("Deve gerar public IDs com o prefixo correto e comprimento padrão")
    void deveGerarComPrefixoCorreto() {
        String bolsistaId = PublicIdGenerator.generateBolsistaId();
        assertNotNull(bolsistaId);
        assertTrue(bolsistaId.startsWith("bol_"));
        assertEquals(24, bolsistaId.length()); // "bol_" (4) + 20 chars = 24

        String professorId = PublicIdGenerator.generateProfessorId();
        assertTrue(professorId.startsWith("prf_"));
        assertEquals(24, professorId.length());

        String adminId = PublicIdGenerator.generateAdministradorId();
        assertTrue(adminId.startsWith("adm_"));

        String usuarioId = PublicIdGenerator.generateUsuarioId();
        assertTrue(usuarioId.startsWith("usr_"));

        String labId = PublicIdGenerator.generateLaboratorioId();
        assertTrue(labId.startsWith("lab_"));

        String projId = PublicIdGenerator.generateProjetoId();
        assertTrue(projId.startsWith("prj_"));

        String cursoId = PublicIdGenerator.generateCursoId();
        assertTrue(cursoId.startsWith("cur_"));

        String freqId = PublicIdGenerator.generateFrequenciaId();
        assertTrue(freqId.startsWith("frq_"));
    }

    @Test
    @DisplayName("Deve gerar IDs únicos sem colisões")
    void deveGerarIdsUnicos() {
        Set<String> ids = new HashSet<>();
        int quantidade = 1000;
        for (int i = 0; i < quantidade; i++) {
            ids.add(PublicIdGenerator.generateBolsistaId());
        }
        assertEquals(quantidade, ids.size());
    }

    @Test
    @DisplayName("Deve validar formato de public ID corretamente")
    void deveValidarPublicId() {
        String idValido = PublicIdGenerator.generateBolsistaId();
        assertTrue(PublicIdGenerator.isValid(idValido, "bol"));
        assertFalse(PublicIdGenerator.isValid(idValido, "prf"));
        assertFalse(PublicIdGenerator.isValid(null, "bol"));
        assertFalse(PublicIdGenerator.isValid("", "bol"));
        assertFalse(PublicIdGenerator.isValid("bol_invalid!char", "bol"));
        assertFalse(PublicIdGenerator.isValid("invalidprefix_12345678901234567890", "bol"));
    }

    @Test
    @DisplayName("Deve lançar exceção para prefixo nulo ou tamanho inválido")
    void deveLancarExcecaoParaEntradasInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> PublicIdGenerator.generate(null));
        assertThrows(IllegalArgumentException.class, () -> PublicIdGenerator.generate(""));
        assertThrows(IllegalArgumentException.class, () -> PublicIdGenerator.generate("bol", 5));
    }
}
