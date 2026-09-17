package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Bolsista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT b FROM Bolsista b WHERE b.laboratorioId = :labId AND b.ativo = true ORDER BY b.nome")
    List<Bolsista> buscarPorLaboratorio(@Param("labId") UUID labId);

    @Query(value = "SELECT b.* FROM bolsista b "
            + "INNER JOIN bolsista_projeto bp ON b.id = bp.bolsista_id "
            + "WHERE bp.projeto_id = :projetoId AND b.ativo = true ORDER BY b.nome",
            nativeQuery = true)
    List<Bolsista> buscarPorProjeto(@Param("projetoId") UUID projetoId);

    int countByTipoUsuarioAndAtivoTrue(String tipoUsuario);

    @Modifying
    @Query("UPDATE Bolsista b SET b.ativo = false WHERE b.id = :id")
    int desativar(@Param("id") UUID id);
}
