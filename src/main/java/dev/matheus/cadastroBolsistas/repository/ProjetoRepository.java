package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Projeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/*
 * a busca com nome/laboratorio opcionais vem de Filtros.projeto(...) via
 * JpaSpecificationExecutor; o resto sao consultas derivadas do nome do metodo.
 */
@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, UUID>, JpaSpecificationExecutor<Projeto> {

    List<Projeto> findByLaboratorioIdAndAtivoTrueOrderByNome(UUID laboratorioId);

    /* navega pelo N:N mapeado em Projeto.bolsistas. */
    List<Projeto> findByBolsistas_IdAndAtivoTrueOrderByNome(UUID bolsistaId);
}
