package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.repository.ProfessorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        when(repository.desativar(id)).thenReturn(1);

        assertTrue(professorService.excluir(id));
        verify(repository).desativar(id);
        verify(repository, never()).deleteById(any(UUID.class));
    }

    @Test
    void excluir_quandoNadaFoiAtualizado_retornaFalse() throws SQLException {
        UUID id = UUID.randomUUID();
        when(repository.desativar(id)).thenReturn(0);

        assertFalse(professorService.excluir(id));
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
}
