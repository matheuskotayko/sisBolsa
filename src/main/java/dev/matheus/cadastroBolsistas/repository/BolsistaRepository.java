package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Bolsista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BolsistaRepository extends JpaRepository<Bolsista, UUID> {

    Optional<Bolsista> findByEmailAndAtivoTrue(String email);

    List<Bolsista> findByAtivoTrueOrderByNome();

    List<Bolsista> findByNomeContainingIgnoreCaseAndAtivoTrueOrderByNome(String nome);

    List<Bolsista> findByCursoContainingIgnoreCaseAndAtivoTrueOrderByNome(String curso);

    List<Bolsista> findByLaboratorioIdAndAtivoTrueOrderByNome(UUID laboratorioId);

    /* navega pelo N:N mapeado em Bolsista.projetos - dispensa o JOIN nativo na bolsista_projeto. */
    List<Bolsista> findByProjetos_IdAndAtivoTrueOrderByNome(UUID projetoId);

    int countByProjetos_IdAndAtivoTrue(UUID projetoId);

    int countByLaboratorioIdAndAtivoTrue(UUID laboratorioId);

    int countByTipoUsuarioAndAtivoTrue(String tipoUsuario);
}
