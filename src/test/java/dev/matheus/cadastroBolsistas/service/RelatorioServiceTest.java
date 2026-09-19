package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.repository.RelatorioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private RelatorioRepository repository;

    @InjectMocks
    private RelatorioService relatorioService;

    @Test
    void exigirAdmin_admin_naoLancaNada() {
        Professor admin = new Professor();
        admin.setTipoUsuario("ADMIN");

        assertDoesNotThrow(() -> relatorioService.exigirAdmin(admin));
    }

    @Test
    void exigirAdmin_naoAdmin_lancaPermissaoNegada() {
        Bolsista bolsista = new Bolsista();
        bolsista.setTipoUsuario("BOLSISTA");

        assertThrows(PermissaoNegadaException.class, () -> relatorioService.exigirAdmin(bolsista));
    }
}
