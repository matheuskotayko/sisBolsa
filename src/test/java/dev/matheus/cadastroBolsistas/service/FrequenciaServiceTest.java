package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Frequencia;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.repository.FrequenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FrequenciaServiceTest {

    @Mock
    private FrequenciaRepository repository;

    @Mock
    private BolsistaService bolsistaService;

    @Mock
    private LaboratorioService laboratorioService;

    @InjectMocks
    private FrequenciaService frequenciaService;

    @Test
    void registrar_preencheOBolsistaParaAResposta() {
        UUID bolsistaId = UUID.randomUUID();
        Bolsista b = new Bolsista();
        b.setId(bolsistaId);
        b.setNome("Lucas Oliveira");
        when(bolsistaService.buscarPorId(bolsistaId)).thenReturn(b);
        Frequencia f = new Frequencia();
        f.setBolsistaId(bolsistaId);

        frequenciaService.registrar(f);

        assertTrue(f.isAtivo());
        assertEquals("Lucas Oliveira", f.getNomeBolsista());
    }

    @Test
    void buscarOuFalhar_inexistente_lancaRecursoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> frequenciaService.buscarOuFalhar(id));
    }

    @Test
    void buscarOuFalhar_existente_retornaAFrequencia() {
        UUID id = UUID.randomUUID();
        Frequencia f = new Frequencia();
        f.setId(id);
        when(repository.findByIdAndAtivoTrue(id)).thenReturn(Optional.of(f));

        assertSame(f, frequenciaService.buscarOuFalhar(id));
    }

    @Test
    void exigirAcesso_admin_naoLancaNada() {
        Professor admin = new Professor();
        admin.setTipoUsuario("ADMIN");

        assertDoesNotThrow(() -> frequenciaService.exigirAcesso(admin, UUID.randomUUID()));
    }

    @Test
    void exigirAcesso_bolsistaVendoOutroBolsista_lancaPermissaoNegada() {
        Bolsista logado = new Bolsista();
        logado.setId(UUID.randomUUID());
        logado.setTipoUsuario("BOLSISTA");

        assertThrows(PermissaoNegadaException.class, () -> frequenciaService.exigirAcesso(logado, UUID.randomUUID()));
    }

    @Test
    void buscarComPermissao_semAcesso_lancaPermissaoNegada() {
        UUID id = UUID.randomUUID();
        UUID bolsistaId = UUID.randomUUID();
        Frequencia f = new Frequencia();
        f.setId(id);
        f.setBolsistaId(bolsistaId);
        when(repository.findByIdAndAtivoTrue(id)).thenReturn(Optional.of(f));

        Bolsista outroBolsista = new Bolsista();
        outroBolsista.setId(UUID.randomUUID());
        outroBolsista.setTipoUsuario("BOLSISTA");

        assertThrows(PermissaoNegadaException.class, () -> frequenciaService.buscarComPermissao(id, outroBolsista));
    }

    @Test
    void buscarComPermissao_donoDoRegistro_retornaAFrequencia() {
        UUID id = UUID.randomUUID();
        UUID bolsistaId = UUID.randomUUID();
        Frequencia f = new Frequencia();
        f.setId(id);
        f.setBolsistaId(bolsistaId);
        when(repository.findByIdAndAtivoTrue(id)).thenReturn(Optional.of(f));

        Bolsista dono = new Bolsista();
        dono.setId(bolsistaId);
        dono.setTipoUsuario("BOLSISTA");

        assertSame(f, frequenciaService.buscarComPermissao(id, dono));
    }
}
