package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Professor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.matheus.cadastroBolsistas.repository.LaboratorioRepository;
import dev.matheus.cadastroBolsistas.repository.ProjetoRepository;

import java.sql.SQLException;
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

    @InjectMocks
    private LaboratorioService laboratorioService;

    @Test
    void podeGerenciar_adminSempreRetornaTrue() throws SQLException {
        Professor admin = new Professor();
        admin.setId(UUID.randomUUID());
        admin.setTipoUsuario("ADMIN");

        assertTrue(laboratorioService.podeGerenciar(admin, UUID.randomUUID()));
        verifyNoInteractions(repository);
    }

    @Test
    void podeGerenciar_professorCoordenadorRetornaTrue() throws SQLException {
        UUID profId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();

        Professor professor = new Professor();
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

        Professor professor = new Professor();
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

        Professor professor = new Professor();
        professor.setId(profId);
        professor.setTipoUsuario("PROFESSOR");

        when(repository.findById(labId)).thenReturn(Optional.empty());

        assertFalse(laboratorioService.podeGerenciar(professor, labId));
        verify(repository).findById(labId);
    }

    @Test
    void podeGerenciar_bolsistaRetornaFalse() throws SQLException {
        Bolsista bolsista = new Bolsista();
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
        when(repository.contarBolsistasAtivos(labId)).thenReturn(5);

        assertTrue(laboratorioService.temVaga(labId));
        verify(repository).findById(labId);
        verify(repository).contarBolsistasAtivos(labId);
    }

    @Test
    void temVaga_retornaFalseQuandoLabEstaLotado() throws SQLException {
        UUID labId = UUID.randomUUID();
        Laboratorio lab = new Laboratorio();
        lab.setId(labId);
        lab.setCapacidade(10);

        when(repository.findById(labId)).thenReturn(Optional.of(lab));
        when(repository.contarBolsistasAtivos(labId)).thenReturn(10);

        assertFalse(laboratorioService.temVaga(labId));
        verify(repository).findById(labId);
        verify(repository).contarBolsistasAtivos(labId);
    }

    @Test
    void temVaga_retornaFalseQuandoLabNaoExiste() throws SQLException {
        UUID labId = UUID.randomUUID();
        when(repository.findById(labId)).thenReturn(Optional.empty());

        assertFalse(laboratorioService.temVaga(labId));
        verify(repository).findById(labId);
        verify(repository, never()).contarBolsistasAtivos(any(UUID.class));
    }
}
