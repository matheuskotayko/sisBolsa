package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Laboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/*
 * a busca por termo (nome, area de pesquisa ou nome do coordenador) vem de
 * Filtros.laboratorio(...) via JpaSpecificationExecutor.
 */
@Repository
public interface LaboratorioRepository extends JpaRepository<Laboratorio, UUID>, JpaSpecificationExecutor<Laboratorio> {

    List<Laboratorio> findByAtivoTrueOrderByNome();

    List<Laboratorio> findByCoordenadorIdAndAtivoTrueOrderByNome(UUID coordenadorId);
}
