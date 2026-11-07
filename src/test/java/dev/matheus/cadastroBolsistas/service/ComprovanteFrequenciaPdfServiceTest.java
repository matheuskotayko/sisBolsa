package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.response.ComprovanteResponse;
import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComprovanteFrequenciaPdfServiceTest {

    @Mock private FrequenciaService frequenciaService;
    @Mock private LaboratorioService laboratorioService;
    @Mock private ProfessorService professorService;
    @InjectMocks private ComprovanteFrequenciaPdfService service;

    private Bolsista bolsista() {
        Bolsista b = new Bolsista();
        b.setId(UUID.randomUUID());
        b.setNome("Maria Silva");
        b.setMatricula("2024001");
        return b;
    }

    @Test
    void emitir_semPeriodo_usaMesCorrenteENomeiaPelaMatricula() throws Exception {
        Bolsista b = bolsista();
        Usuario logado = new Usuario();
        when(frequenciaService.resolverBolsistaAlvo(logado, (String) null)).thenReturn(b);
        when(frequenciaService.buscarFrequencias(any(UUID.class), any(LocalDate.class), any(LocalDate.class), any(), any()))
                .thenReturn(new java.util.ArrayList<>(List.of()));

        ComprovanteResponse r = service.emitir(logado, null, null, null);

        LocalDate hoje = LocalDate.now();
        verify(frequenciaService).buscarFrequencias(b.getId(), hoje.withDayOfMonth(1),
                hoje.withDayOfMonth(hoje.lengthOfMonth()), null, null);
        assertEquals("comprovante_frequencia_2024001.pdf", r.nomeArquivo());
        assertTrue(r.conteudo().length > 0);
    }

    @Test
    void emitir_semAcesso_naoGeraPdf() {
        Bolsista b = bolsista();
        Usuario logado = new Usuario();
        when(frequenciaService.resolverBolsistaAlvo(logado, "bol_x")).thenReturn(b);
        doThrow(new PermissaoNegadaException("negado")).when(frequenciaService).exigirAcesso(logado, b.getId());

        assertThrows(PermissaoNegadaException.class, () -> service.emitir(logado, "bol_x", null, null));
        verify(frequenciaService, never()).buscarFrequencias(any(UUID.class), any(), any(), any(), any());
    }
}
