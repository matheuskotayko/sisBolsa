package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Laboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LaboratorioRepository extends JpaRepository<Laboratorio, UUID> {

    List<Laboratorio> findByAtivoTrueOrderByNome();

    @Query("SELECT l FROM Laboratorio l LEFT JOIN l.coordenadorProfessor c WHERE l.ativo = true "
            + "AND (LOWER(l.nome) LIKE LOWER(CONCAT('%', :buscaNome, '%')) "
            + "  OR LOWER(l.areaPesquisa) LIKE LOWER(CONCAT('%', :buscaNome, '%')) "
            + "  OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :buscaNome, '%'))) "
            + "ORDER BY l.nome")
    List<Laboratorio> buscarLaboratorios(@Param("buscaNome") String buscaNome);

    @Query("SELECT l FROM Laboratorio l WHERE l.coordenadorId = :professorId AND l.ativo = true ORDER BY l.nome")
    List<Laboratorio> buscarPorCoordenador(@Param("professorId") UUID professorId);

    @Query("SELECT COUNT(b) FROM Bolsista b WHERE b.laboratorioId = :labId AND b.ativo = true")
    int contarBolsistasAtivos(@Param("labId") UUID labId);

    @Modifying
    @Query("UPDATE Laboratorio l SET l.ativo = false WHERE l.id = :id")
    int desativar(@Param("id") UUID id);
}
