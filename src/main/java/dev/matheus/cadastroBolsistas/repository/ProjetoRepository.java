package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Projeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {

    @Query("SELECT p FROM Projeto p WHERE p.ativo = true "
            + "AND (LOWER(p.nome) LIKE LOWER(CONCAT('%', :buscaNome, '%')) "
            + "  OR LOWER(p.descricao) LIKE LOWER(CONCAT('%', :buscaNome, '%'))) "
            + "AND (:labId IS NULL OR p.laboratorioId = :labId) "
            + "ORDER BY p.nome")
    List<Projeto> buscarProjetos(@Param("buscaNome") String buscaNome, @Param("labId") UUID labId);

    @Query("SELECT p FROM Projeto p WHERE p.laboratorioId = :labId AND p.ativo = true ORDER BY p.nome")
    List<Projeto> buscarPorLaboratorio(@Param("labId") UUID labId);

    @Query(value = "SELECT p.* FROM projeto p "
            + "INNER JOIN bolsista_projeto bp ON p.id = bp.projeto_id "
            + "WHERE bp.bolsista_id = :bolsistaId AND p.ativo = true ORDER BY p.nome",
            nativeQuery = true)
    List<Projeto> buscarPorBolsista(@Param("bolsistaId") UUID bolsistaId);

    @Query(value = "SELECT bp.bolsista_id, bp.projeto_id FROM bolsista_projeto bp "
            + "INNER JOIN bolsista b ON bp.bolsista_id = b.id "
            + "INNER JOIN projeto p ON bp.projeto_id = p.id "
            + "WHERE b.laboratorio_id = :labId AND p.ativo = true",
            nativeQuery = true)
    List<Object[]> buscarVinculosDoLaboratorio(@Param("labId") UUID labId);

    @Query(value = "SELECT COUNT(*) FROM bolsista_projeto bp "
            + "INNER JOIN bolsista b ON bp.bolsista_id = b.id "
            + "WHERE bp.projeto_id = :projetoId AND b.ativo = true",
            nativeQuery = true)
    int contarMembros(@Param("projetoId") UUID projetoId);

    @Modifying
    @Query(value = "INSERT INTO bolsista_projeto (bolsista_id, projeto_id) VALUES (:bolsistaId, :projetoId) "
            + "ON CONFLICT DO NOTHING", nativeQuery = true)
    int vincularBolsista(@Param("bolsistaId") UUID bolsistaId, @Param("projetoId") UUID projetoId);

    @Modifying
    @Query(value = "DELETE FROM bolsista_projeto WHERE bolsista_id = :bolsistaId AND projeto_id = :projetoId",
            nativeQuery = true)
    int desvincularBolsista(@Param("bolsistaId") UUID bolsistaId, @Param("projetoId") UUID projetoId);

    @Modifying
    @Query(value = "DELETE FROM bolsista_projeto WHERE bolsista_id = :bolsistaId", nativeQuery = true)
    int desvincularBolsistaDeTodosProjetos(@Param("bolsistaId") UUID bolsistaId);

    @Modifying
    @Query("UPDATE Projeto p SET p.ativo = false WHERE p.id = :id")
    int desativar(@Param("id") UUID id);
}
