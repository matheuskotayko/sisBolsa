package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Frequencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
 * os filtros por bolsista e periodo (todos opcionais) vem de Filtros.frequencia*
 * via JpaSpecificationExecutor.
 */
@Repository
public interface FrequenciaRepository extends JpaRepository<Frequencia, UUID>, JpaSpecificationExecutor<Frequencia> {

    Optional<Frequencia> findByIdAndAtivoTrue(UUID id);

    List<Frequencia> findByBolsistaIdAndAtivoTrueOrderByDataDesc(UUID bolsistaId);

    /* atravessa a associacao Frequencia -> Bolsista pra filtrar pelo laboratorio dele. */
    List<Frequencia> findByBolsista_LaboratorioIdAndAtivoTrueOrderByDataDesc(UUID laboratorioId);

    List<Frequencia> findByAtivoTrueOrderByDataDesc();
}
