package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Frequencia;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FrequenciaRepository extends JpaRepository<Frequencia, UUID> {

    Optional<Frequencia> findByIdAndAtivoTrue(UUID id);

    @Query("SELECT f FROM Frequencia f WHERE f.bolsistaId = :bolsistaId AND f.ativo = true ORDER BY f.data DESC")
    List<Frequencia> buscarPorBolsista(@Param("bolsistaId") UUID bolsistaId);

    @Query("SELECT f FROM Frequencia f WHERE f.ativo = true "
            + "AND f.bolsistaId IN (SELECT b.id FROM Bolsista b WHERE b.laboratorioId = :labId) "
            + "ORDER BY f.data DESC")
    List<Frequencia> buscarPorLaboratorio(@Param("labId") UUID labId);

    List<Frequencia> findByAtivoTrueOrderByDataDesc();

    @Query("SELECT f FROM Frequencia f WHERE f.ativo = true "
            + "AND (:bolsistaId IS NULL OR f.bolsistaId = :bolsistaId) "
            + "AND (cast(:dataInicio AS date) IS NULL OR f.data >= :dataInicio) "
            + "AND (cast(:dataFim AS date) IS NULL OR f.data <= :dataFim) "
            + "ORDER BY f.data DESC")
    List<Frequencia> buscarFrequencias(@Param("bolsistaId") UUID bolsistaId,
                                       @Param("dataInicio") LocalDate dataInicio,
                                       @Param("dataFim") LocalDate dataFim,
                                       Pageable pageable);

    @Query("SELECT COUNT(f) FROM Frequencia f WHERE f.ativo = true "
            + "AND (:bolsistaId IS NULL OR f.bolsistaId = :bolsistaId) "
            + "AND (cast(:dataInicio AS date) IS NULL OR f.data >= :dataInicio) "
            + "AND (cast(:dataFim AS date) IS NULL OR f.data <= :dataFim)")
    int contarFrequencias(@Param("bolsistaId") UUID bolsistaId,
                          @Param("dataInicio") LocalDate dataInicio,
                          @Param("dataFim") LocalDate dataFim);

    @Query("SELECT f FROM Frequencia f WHERE f.ativo = true AND f.bolsistaId IN :ids "
            + "AND (cast(:dataInicio AS date) IS NULL OR f.data >= :dataInicio) "
            + "AND (cast(:dataFim AS date) IS NULL OR f.data <= :dataFim) "
            + "ORDER BY f.data DESC")
    List<Frequencia> buscarPorBolsistas(@Param("ids") List<UUID> ids,
                                        @Param("dataInicio") LocalDate dataInicio,
                                        @Param("dataFim") LocalDate dataFim,
                                        Pageable pageable);

    @Query("SELECT COUNT(f) FROM Frequencia f WHERE f.ativo = true AND f.bolsistaId IN :ids "
            + "AND (cast(:dataInicio AS date) IS NULL OR f.data >= :dataInicio) "
            + "AND (cast(:dataFim AS date) IS NULL OR f.data <= :dataFim)")
    int contarPorBolsistas(@Param("ids") List<UUID> ids,
                           @Param("dataInicio") LocalDate dataInicio,
                           @Param("dataFim") LocalDate dataFim);

    @Modifying
    @Query("UPDATE Frequencia f SET f.ativo = false WHERE f.id = :id")
    int desativar(@Param("id") UUID id);
}
