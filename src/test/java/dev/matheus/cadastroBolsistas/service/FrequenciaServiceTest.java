package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.ResumoHorasResponse;
import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Frequencia;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.FrequenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
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
        Usuario admin = new Usuario();
        admin.setTipoUsuario("ADMIN");

        assertDoesNotThrow(() -> frequenciaService.exigirAcesso(admin, UUID.randomUUID()));
    }

    @Test
    void exigirAcesso_bolsistaVendoOutroBolsista_lancaPermissaoNegada() {
        Usuario logado = new Usuario();
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

        Usuario outroBolsista = new Usuario();
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

        Usuario dono = new Usuario();
        dono.setId(bolsistaId);
        dono.setTipoUsuario("BOLSISTA");

        assertSame(f, frequenciaService.buscarComPermissao(id, dono));
    }

    @Test
    void calcularResumoHoras_paraBolsista_retornaHorasMesETotal() {
        UUID bolsistaId = UUID.randomUUID();
        Usuario bolsistaLogado = new Usuario();
        bolsistaLogado.setId(bolsistaId);
        bolsistaLogado.setTipoUsuario("BOLSISTA");

        LocalDate hoje = LocalDate.now();
        LocalDate mesPassado = hoje.minusMonths(1);

        Frequencia f1 = new Frequencia();
        f1.setBolsistaId(bolsistaId);
        f1.setData(hoje);
        f1.setHorasTrabalhadas(4.0);

        Frequencia f2 = new Frequencia();
        f2.setBolsistaId(bolsistaId);
        f2.setData(mesPassado);
        f2.setHorasTrabalhadas(6.0);

        when(repository.findByBolsistaIdAndAtivoTrueOrderByDataDesc(bolsistaId)).thenReturn(List.of(f1, f2));

        ResumoHorasResponse resumo = frequenciaService.calcularResumoHoras(bolsistaLogado, null);

        assertEquals(4.0, resumo.horasMes());
        assertEquals(10.0, resumo.horasTotal());
    }

    @Test
    void gerarCsv_paraAdmin_retornaCabecalhoELinhas() {
        Usuario admin = new Usuario();
        admin.setId(UUID.randomUUID());
        admin.setTipoUsuario("ADMIN");

        Frequencia f = new Frequencia();
        f.setId(UUID.randomUUID());
        f.setPublicId("frq_123");
        Bolsista b = new Bolsista();
        b.setNome("Lucas");
        f.setBolsista(b);
        f.setData(LocalDate.of(2026, 9, 20));
        f.setHorasTrabalhadas(4.0);
        f.setDescricao("Desenvolvimento do backend");
        f.setLinkComprovante("https://link.com");

        Page<Frequencia> page = new PageImpl<>(List.of(f));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        String csv = frequenciaService.gerarCsv(admin, null, null, null);

        assertTrue(csv.contains("ID,Bolsista,Data,Horas Trabalhadas,Descricao,LinkComprovante"));
        assertTrue(csv.contains("frq_123"));
        assertTrue(csv.contains("\"Lucas\""));
        assertTrue(csv.contains("4.0"));
    }
}
