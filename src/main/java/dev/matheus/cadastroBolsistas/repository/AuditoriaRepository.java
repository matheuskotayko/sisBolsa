package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/*
 * a consulta de logs filtra por entidade, acao e periodo, todos opcionais:
 * Filtros.auditoria(...) monta o WHERE em Criteria API e o proprio
 * JpaSpecificationExecutor da o findAll paginado e o count.
 */
@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID>, JpaSpecificationExecutor<Auditoria> {
}
