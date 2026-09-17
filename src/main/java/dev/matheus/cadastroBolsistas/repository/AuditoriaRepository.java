package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Auditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {

    @Query("SELECT a FROM Auditoria a WHERE "
            + "(:entidade IS NULL OR a.entidade = :entidade) "
            + "AND (:acao IS NULL OR a.acao = :acao) "
            + "AND (cast(:dataInicio AS timestamp) IS NULL OR a.dataHora >= :dataInicio) "
            + "AND (cast(:dataFim AS timestamp) IS NULL OR a.dataHora <= :dataFim) "
            + "ORDER BY a.dataHora DESC")
    List<Auditoria> buscarLogs(@Param("entidade") String entidade,
                               @Param("acao") String acao,
                               @Param("dataInicio") LocalDateTime dataInicio,
                               @Param("dataFim") LocalDateTime dataFim,
                               Pageable pageable);

    @Query("SELECT COUNT(a) FROM Auditoria a WHERE "
            + "(:entidade IS NULL OR a.entidade = :entidade) "
            + "AND (:acao IS NULL OR a.acao = :acao) "
            + "AND (cast(:dataInicio AS timestamp) IS NULL OR a.dataHora >= :dataInicio) "
            + "AND (cast(:dataFim AS timestamp) IS NULL OR a.dataHora <= :dataFim)")
    int contarLogs(@Param("entidade") String entidade,
                   @Param("acao") String acao,
                   @Param("dataInicio") LocalDateTime dataInicio,
                   @Param("dataFim") LocalDateTime dataFim);
}
