package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Curso;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.repository.CursoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository repository;

    @InjectMocks
    private CursoService cursoService;

    private Professor admin() {
        Professor p = new Professor();
        p.setTipoUsuario("ADMIN");
        return p;
    }

    @Test
    void cadastrar_naoAdmin_lancaPermissaoNegadaSemConsultarRepositorio() {
        Bolsista bolsista = new Bolsista();
        bolsista.setTipoUsuario("BOLSISTA");

        assertThrows(PermissaoNegadaException.class, () -> cursoService.cadastrar("Engenharia de Software", bolsista));
        verifyNoInteractions(repository);
    }

    @Test
    void cadastrar_adminComNomeDuplicado_lancaIllegalArgument() {
        when(repository.existsByNomeIgnoreCaseAndAtivoTrue("Engenharia de Software")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> cursoService.cadastrar("Engenharia de Software", admin()));
        verify(repository, never()).save(any());
    }

    @Test
    void cadastrar_adminComNomeNovo_salvaEAtiva() {
        when(repository.existsByNomeIgnoreCaseAndAtivoTrue("Engenharia de Software")).thenReturn(false);
        when(repository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

        Curso resultado = cursoService.cadastrar("Engenharia de Software", admin());

        assertEquals("Engenharia de Software", resultado.getNome());
        assertTrue(resultado.isAtivo());
        ArgumentCaptor<Curso> captor = ArgumentCaptor.forClass(Curso.class);
        verify(repository).save(captor.capture());
        assertTrue(captor.getValue().isAtivo());
    }
}
