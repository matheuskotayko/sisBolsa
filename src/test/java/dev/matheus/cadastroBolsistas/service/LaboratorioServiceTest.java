package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.BolsistaRepository;
import dev.matheus.cadastroBolsistas.repository.LaboratorioRepository;
import dev.matheus.cadastroBolsistas.repository.ProjetoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaboratorioServiceTest {

    @Mock
    private LaboratorioRepository repository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private BolsistaRepository bolsistaRepository;

    @InjectMocks
    private LaboratorioService laboratorioService;

    @Test
    void podeGerenciar_adminSempreRetornaTrue() throws SQLException {
        Usuario admin = new Usuario();
        admin.setId(UUID.randomUUID());
        admin.setTipoUsuario("ADMIN");

        assertTrue(laboratorioService.podeGerenciar(admin, UUID.randomUUID()));
        verifyNoInteractions(repository);
    }

    @Test
    void podeGerenciar_professorCoordenadorRetornaTrue() throws SQLException {
        UUID profId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();

        Usuario professor = new Usuario();
        professor.setId(profId);
        professor.setTipoUsuario("PROFESSOR");

        Laboratorio lab = new Laboratorio();
        lab.setId(labId);
        lab.setCoordenadorId(profId);

        when(repository.findById(labId)).thenReturn(Optional.of(lab));

        assertTrue(laboratorioService.podeGerenciar(professor, labId));
        verify(repository).findById(labId);
    }

    @Test
    void podeGerenciar_professorNaoCoordenadorRetornaFalse() throws SQLException {
        UUID profId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();

        Usuario professor = new Usuario();
        professor.setId(profId);
        professor.setTipoUsuario("PROFESSOR");

        Laboratorio lab = new Laboratorio();
        lab.setId(labId);
        lab.setCoordenadorId(UUID.randomUUID());

        when(repository.findById(labId)).thenReturn(Optional.of(lab));

        assertFalse(laboratorioService.podeGerenciar(professor, labId));
        verify(repository).findById(labId);
    }

    @Test
    void podeGerenciar_professorLabInexistenteRetornaFalse() throws SQLException {
        UUID profId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();

        Usuario professor = new Usuario();
        professor.setId(profId);
        professor.setTipoUsuario("PROFESSOR");

        when(repository.findById(labId)).thenReturn(Optional.empty());

        assertFalse(laboratorioService.podeGerenciar(professor, labId));
        verify(repository).findById(labId);
    }

    @Test
    void podeGerenciar_bolsistaRetornaFalse() throws SQLException {
        Usuario bolsista = new Usuario();
        bolsista.setId(UUID.randomUUID());
        bolsista.setTipoUsuario("BOLSISTA");

        assertFalse(laboratorioService.podeGerenciar(bolsista, UUID.randomUUID()));
        verifyNoInteractions(repository);
    }

    @Test
    void podeGerenciar_usuarioNullRetornaFalse() throws SQLException {
        assertFalse(laboratorioService.podeGerenciar(null, UUID.randomUUID()));
        verifyNoInteractions(repository);
    }

    @Test
    void temVaga_retornaTrueQuandoLabTemEspaco() throws SQLException {
        UUID labId = UUID.randomUUID();
        Laboratorio lab = new Laboratorio();
        lab.setId(labId);
        lab.setCapacidade(10);

        when(repository.findById(labId)).thenReturn(Optional.of(lab));
        when(bolsistaRepository.countByLaboratorioIdAndAtivoTrue(labId)).thenReturn(5);

        assertTrue(laboratorioService.temVaga(labId));
        verify(repository).findById(labId);
        verify(bolsistaRepository).countByLaboratorioIdAndAtivoTrue(labId);
    }

    @Test
    void temVaga_retornaFalseQuandoLabEstaLotado() throws SQLException {
        UUID labId = UUID.randomUUID();
        Laboratorio lab = new Laboratorio();
        lab.setId(labId);
        lab.setCapacidade(10);

        when(repository.findById(labId)).thenReturn(Optional.of(lab));
        when(bolsistaRepository.countByLaboratorioIdAndAtivoTrue(labId)).thenReturn(10);

        assertFalse(laboratorioService.temVaga(labId));
        verify(repository).findById(labId);
        verify(bolsistaRepository).countByLaboratorioIdAndAtivoTrue(labId);
    }

    @Test
    void temVaga_retornaFalseQuandoLabNaoExiste() throws SQLException {
        UUID labId = UUID.randomUUID();
        when(repository.findById(labId)).thenReturn(Optional.empty());

        assertFalse(laboratorioService.temVaga(labId));
        verify(repository).findById(labId);
        verify(bolsistaRepository, never()).countByLaboratorioIdAndAtivoTrue(any(UUID.class));
    }

    @Test
    void cadastrar_salvaEAtivaOLaboratorio() {
        Laboratorio lab = new Laboratorio();

        assertTrue(laboratorioService.cadastrar(lab));
        assertTrue(lab.isAtivo());
        verify(repository).save(lab);
    }

    @Test
    void buscarOuFalhar_labInexistente_lancaRecursoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> laboratorioService.buscarOuFalhar(id));
    }

    @Test
    void buscarOuFalhar_labDesativado_lancaRecursoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        Laboratorio lab = new Laboratorio();
        lab.setId(id);
        lab.setAtivo(false);
        when(repository.findById(id)).thenReturn(Optional.of(lab));
        when(projetoRepository.findByLaboratorioIdAndAtivoTrueOrderByNome(id)).thenReturn(List.of());

        assertThrows(RecursoNaoEncontradoException.class, () -> laboratorioService.buscarOuFalhar(id));
    }

    @Test
    void listar_calculaAOcupacaoDaPaginaComUmaUnicaConsultaAgrupada() {
        Laboratorio a = new Laboratorio();
        a.setId(UUID.randomUUID());
        a.setCapacidade(10);
        Laboratorio b = new Laboratorio();
        b.setId(UUID.randomUUID());
        b.setCapacidade(4);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(a, b)));
        BolsistaRepository.Contagem deA = mock(BolsistaRepository.Contagem.class);
        when(deA.getChave()).thenReturn(a.getId());
        when(deA.getTotal()).thenReturn(5L);
        when(bolsistaRepository.contarPorLaboratorios(any())).thenReturn(List.of(deA));

        Usuario admin = new Usuario();
        admin.setTipoUsuario("ADMIN");
        var pagina = laboratorioService.listar(admin, null, org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(50.0, pagina.getContent().get(0).percentualOcupacao());
        assertEquals(0.0, pagina.getContent().get(1).percentualOcupacao());
        verify(bolsistaRepository, times(1)).contarPorLaboratorios(any());
        verify(bolsistaRepository, never()).countByLaboratorioIdAndAtivoTrue(any());
    }

    @Test
    void buscarExigindoGerencia_semPermissao_lancaPermissaoNegada() {
        UUID id = UUID.randomUUID();
        Laboratorio lab = new Laboratorio();
        lab.setId(id);
        lab.setAtivo(true);
        lab.setCoordenadorId(UUID.randomUUID());
        when(repository.findById(id)).thenReturn(Optional.of(lab));
        when(projetoRepository.findByLaboratorioIdAndAtivoTrueOrderByNome(id)).thenReturn(List.of());

        Usuario outroProfessor = new Usuario();
        outroProfessor.setId(UUID.randomUUID());
        outroProfessor.setTipoUsuario("PROFESSOR");

        assertThrows(PermissaoNegadaException.class, () -> laboratorioService.buscarExigindoGerencia(id, outroProfessor));
    }

    @Test
    void buscarExigindoGerencia_admin_retornaOLaboratorio() {
        UUID id = UUID.randomUUID();
        Laboratorio lab = new Laboratorio();
        lab.setId(id);
        lab.setAtivo(true);
        when(repository.findById(id)).thenReturn(Optional.of(lab));
        when(projetoRepository.findByLaboratorioIdAndAtivoTrueOrderByNome(id)).thenReturn(List.of());

        Usuario admin = new Usuario();
        admin.setTipoUsuario("ADMIN");

        assertSame(lab, laboratorioService.buscarExigindoGerencia(id, admin));
    }
}
