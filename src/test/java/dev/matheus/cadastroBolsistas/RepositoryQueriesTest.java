package dev.matheus.cadastroBolsistas;

import dev.matheus.cadastroBolsistas.repository.AdministradorRepository;
import dev.matheus.cadastroBolsistas.repository.BolsistaRepository;
import dev.matheus.cadastroBolsistas.repository.Filtros;
import dev.matheus.cadastroBolsistas.repository.ProfessorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/*
 * os testes de service mockam os repositorios, entao uma query invalida
 * (ex: b.ativo em vez de b.usuario.ativo) so quebrava em runtime com 500.
 * aqui cada query roda de verdade no postgres - precisa do banco no ar
 * (docker compose up -d db).
 */
@SpringBootTest
class RepositoryQueriesTest {

    @Autowired
    private BolsistaRepository bolsistas;

    @Autowired
    private ProfessorRepository professores;

    @Autowired
    private AdministradorRepository administradores;

    @Test
    void consultasDeBolsistaResolvem() {
        UUID id = UUID.randomUUID();
        assertDoesNotThrow(() -> {
            bolsistas.findByPublicIdAndAtivoTrue("x");
            bolsistas.findByEmailAndAtivoTrue("x");
            bolsistas.findByAtivoTrueOrderByNome();
            bolsistas.findByLaboratorioIdAndAtivoTrueOrderByNome(id);
            bolsistas.findByLaboratorioPublicIdAndAtivoTrueOrderByNome("x");
            bolsistas.findByProjetos_IdAndAtivoTrueOrderByNome(id);
            bolsistas.findByProjetosPublicIdAndAtivoTrueOrderByNome("x");
            bolsistas.countByProjetos_IdAndAtivoTrue(id);
            bolsistas.countByLaboratorioIdAndAtivoTrue(id);
            bolsistas.countByTipoUsuarioAndAtivoTrue("BOLSISTA");
            bolsistas.contarPorLaboratorios(java.util.List.of(id));
            bolsistas.contarPorProjetos(java.util.List.of(id));
            bolsistas.findAll(Filtros.bolsista("a", "a", "BOLSISTA"),
                    Filtros.ordenado(PageRequest.of(0, 5), Sort.by("usuario.nome")));
        });
    }

    @Test
    void consultasDeProfessorEAdminResolvem() {
        assertDoesNotThrow(() -> {
            professores.findByPublicIdAndAtivoTrue("x");
            professores.findByEmailAndAtivoTrue("x");
            professores.findByAtivoTrueOrderByNome();
            professores.findAll(Filtros.professor("a"), PageRequest.of(0, 5));
            administradores.findByPublicIdAndAtivoTrue("x");
            administradores.findByEmailAndAtivoTrue("x");
            administradores.findByAtivoTrueOrderByNome();
            administradores.countAtivos();
        });
    }
}
