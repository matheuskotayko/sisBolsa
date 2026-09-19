package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.repository.AuditoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository repository;

    @InjectMocks
    private AuditoriaService auditoriaService;

    @Test
    void exigirAcesso_admin_naoLancaNada() {
        Professor admin = new Professor();
        admin.setTipoUsuario("ADMIN");

        assertDoesNotThrow(() -> auditoriaService.exigirAcesso(admin));
    }

    @Test
    void exigirAcesso_professor_naoLancaNada() {
        Professor professor = new Professor();
        professor.setTipoUsuario("PROFESSOR");

        assertDoesNotThrow(() -> auditoriaService.exigirAcesso(professor));
    }

    @Test
    void exigirAcesso_bolsista_lancaPermissaoNegada() {
        Bolsista bolsista = new Bolsista();
        bolsista.setTipoUsuario("BOLSISTA");

        assertThrows(PermissaoNegadaException.class, () -> auditoriaService.exigirAcesso(bolsista));
    }
}
