package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.ProfessorRequest;
import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.repository.LaboratorioRepository;
import dev.matheus.cadastroBolsistas.repository.ProfessorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfessorServiceTest {

    @Mock
    private ProfessorRepository repository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfessorService professorService;

    @Test
    void inserir_salvaNoRepositorio() throws SQLException {
        Professor p = new Professor();
        p.setNome("Prof Roberto");

        assertTrue(professorService.inserir(p));
        verify(repository).save(p);
    }

    @Test
    void listarTodos_retornaListaDoRepositorio() throws SQLException {
        when(repository.findByAtivoTrueOrderByNome())
                .thenReturn(List.of(new Professor(), new Professor()));

        ArrayList<Professor> resultado = professorService.listarTodos();

        assertEquals(2, resultado.size());
        verify(repository).findByAtivoTrueOrderByNome();
    }

    @Test
    void listarTodos_semProfessores_retornaListaVazia() throws SQLException {
        when(repository.findByAtivoTrueOrderByNome()).thenReturn(List.of());

        assertTrue(professorService.listarTodos().isEmpty());
    }

    @Test
    void buscarPorId_professorExistente_retornaObjeto() throws SQLException {
        UUID id = UUID.randomUUID();
        Professor p = new Professor();
        p.setId(id);
        p.setNome("Prof Roberto");
        when(repository.findById(id)).thenReturn(Optional.of(p));

        Professor resultado = professorService.buscarPorId(id);

        assertNotNull(resultado);
        assertEquals(id, resultado.getId());
        assertEquals("Prof Roberto", resultado.getNome());
        verify(repository).findById(id);
    }

    @Test
    void buscarPorId_professorInexistente_retornaNull() throws SQLException {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertNull(professorService.buscarPorId(id));
        verify(repository).findById(id);
    }

    @Test
    void atualizar_salvaNoRepositorio() throws SQLException {
        Professor p = new Professor();
        p.setId(UUID.randomUUID());

        assertTrue(professorService.atualizar(p));
        verify(repository).save(p);
    }

    @Test
    void excluir_fazSoftDeleteEmVezDeApagarALinha() throws SQLException {
        UUID id = UUID.randomUUID();
        Professor p = new Professor();
        p.setId(id);
        p.setAtivo(true);
        when(repository.findById(id)).thenReturn(Optional.of(p));

        assertTrue(professorService.excluir(id));
        assertFalse(p.isAtivo());
        verify(repository).save(p);
        verify(repository, never()).deleteById(any(UUID.class));
    }

    @Test
    void excluir_quandoNadaFoiAtualizado_retornaFalse() throws SQLException {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertFalse(professorService.excluir(id));
        verify(repository, never()).save(any(Professor.class));
    }

    @Test
    void buscarPorNome_retornaListaFiltrada() throws SQLException {
        Professor p = new Professor();
        p.setNome("Roberto Mendes");
        when(repository.findByNomeContainingIgnoreCaseAndAtivoTrueOrderByNome("Roberto"))
                .thenReturn(List.of(p));

        ArrayList<Professor> resultado = professorService.buscarPorNome("Roberto");

        assertEquals(1, resultado.size());
        assertEquals("Roberto Mendes", resultado.get(0).getNome());
    }

    @Test
    void buscarPorNome_semResultados_retornaListaVazia() throws SQLException {
        when(repository.findByNomeContainingIgnoreCaseAndAtivoTrueOrderByNome("Inexistente"))
                .thenReturn(List.of());

        assertTrue(professorService.buscarPorNome("Inexistente").isEmpty());
    }

    @Test
    void buscarOuFalhar_professorInexistente_lancaRecursoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> professorService.buscarOuFalhar(id));
    }

    @Test
    void buscarOuFalhar_professorExistente_retornaProfessor() {
        UUID id = UUID.randomUUID();
        Professor p = new Professor();
        p.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(p));

        assertSame(p, professorService.buscarOuFalhar(id));
    }

    @Test
    void listarComLaboratorio_enriqueceProfessoresComQueryUnica() {
        UUID profId1 = UUID.randomUUID();
        UUID profId2 = UUID.randomUUID();

        Professor p1 = new Professor();
        p1.setId(profId1);
        p1.setNome("Prof 1");

        Professor p2 = new Professor();
        p2.setId(profId2);
        p2.setNome("Prof 2");

        when(repository.findByAtivoTrueOrderByNome()).thenReturn(List.of(p1, p2));

        Laboratorio lab1 = new Laboratorio();
        lab1.setCoordenadorId(profId1);
        lab1.setNome("Lab Inteligência Artificial");

        when(laboratorioRepository.findByAtivoTrueOrderByNome()).thenReturn(List.of(lab1));

        ArrayList<Professor> resultado = professorService.listarComLaboratorio(null);

        assertEquals(2, resultado.size());
        assertEquals("Lab Inteligência Artificial", p1.getNomeLaboratorio());
        assertNull(p2.getNomeLaboratorio());
        verify(laboratorioRepository, times(1)).findByAtivoTrueOrderByNome();
    }

    @Test
    void buscarComLaboratorio_enriqueceProfessor() {
        UUID profId = UUID.randomUUID();
        Professor p = new Professor();
        p.setId(profId);
        p.setPublicId("prf_123");

        when(repository.findByPublicIdAndAtivoTrue("prf_123")).thenReturn(Optional.of(p));

        Laboratorio lab = new Laboratorio();
        lab.setCoordenadorId(profId);
        lab.setNome("Lab Redes");

        when(laboratorioRepository.findByCoordenadorIdAndAtivoTrueOrderByNome(profId)).thenReturn(List.of(lab));

        Professor resultado = professorService.buscarComLaboratorio("prf_123");

        assertNotNull(resultado);
        assertEquals("Lab Redes", resultado.getNomeLaboratorio());
    }

    @Test
    void criar_validaSenhaCriptografaESalva() {
        ProfessorRequest req = new ProfessorRequest("Prof Roberto", "roberto@teste.com", "senha123", null, null);
        when(passwordEncoder.encode("senha123")).thenReturn("hash123");

        Professor criado = professorService.criar(req);

        assertNotNull(criado);
        assertEquals("Prof Roberto", criado.getNome());
        assertEquals("hash123", criado.getSenha());
        verify(repository).save(criado);
    }

    @Test
    void atualizar_comSenhaNova_criptografaEAtualiza() {
        UUID profId = UUID.randomUUID();
        Professor existente = new Professor();
        existente.setId(profId);
        existente.setPublicId("prf_123");
        existente.setSenha("hashAntigo");

        when(repository.findByPublicIdAndAtivoTrue("prf_123")).thenReturn(Optional.of(existente));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hashNovo");

        ProfessorRequest req = new ProfessorRequest("Prof Roberto Editado", "roberto@teste.com", "novaSenha123", null, null);

        Professor atualizado = professorService.atualizar("prf_123", req);

        assertEquals("Prof Roberto Editado", atualizado.getNome());
        assertEquals("hashNovo", atualizado.getSenha());
        verify(repository).save(existente);
    }
}
