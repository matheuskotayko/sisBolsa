package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Bolsista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BolsistaRepository extends JpaRepository<Bolsista, UUID>, JpaSpecificationExecutor<Bolsista> {

    /** contagem agrupada: uma consulta para a pagina inteira em vez de uma por item. */
    interface Contagem {
        UUID getChave();
        long getTotal();
    }

    @Query("SELECT b.laboratorioId AS chave, COUNT(b) AS total FROM Bolsista b WHERE b.laboratorioId IN :ids AND b.usuario.ativo = true GROUP BY b.laboratorioId")
    List<Contagem> contarPorLaboratorios(@Param("ids") Collection<UUID> ids);

    @Query("SELECT p.id AS chave, COUNT(b) AS total FROM Bolsista b JOIN b.projetos p WHERE p.id IN :ids AND b.usuario.ativo = true GROUP BY p.id")
    List<Contagem> contarPorProjetos(@Param("ids") Collection<UUID> ids);

    Optional<Bolsista> findByPublicId(String publicId);

    @Query("SELECT b FROM Bolsista b WHERE b.publicId = :publicId AND b.usuario.ativo = true")
    Optional<Bolsista> findByPublicIdAndAtivoTrue(@Param("publicId") String publicId);

    @Query("SELECT b FROM Bolsista b WHERE b.usuario.email = :email AND b.usuario.ativo = true")
    Optional<Bolsista> findByEmailAndAtivoTrue(@Param("email") String email);

    @Query("SELECT b FROM Bolsista b WHERE b.usuario.ativo = true ORDER BY b.usuario.nome")
    List<Bolsista> findByAtivoTrueOrderByNome();

    @Query("SELECT b FROM Bolsista b WHERE b.laboratorioId = :laboratorioId AND b.usuario.ativo = true ORDER BY b.usuario.nome")
    List<Bolsista> findByLaboratorioIdAndAtivoTrueOrderByNome(@Param("laboratorioId") UUID laboratorioId);

    @Query("SELECT b FROM Bolsista b WHERE b.laboratorio.publicId = :labPublicId AND b.usuario.ativo = true ORDER BY b.usuario.nome")
    List<Bolsista> findByLaboratorioPublicIdAndAtivoTrueOrderByNome(@Param("labPublicId") String labPublicId);

    /* navega pelo N:N mapeado em Bolsista.projetos - dispensa o JOIN nativo na bolsista_projeto. */
    @Query("SELECT b FROM Bolsista b JOIN b.projetos p WHERE p.id = :projetoId AND b.usuario.ativo = true ORDER BY b.usuario.nome")
    List<Bolsista> findByProjetos_IdAndAtivoTrueOrderByNome(@Param("projetoId") UUID projetoId);

    @Query("SELECT b FROM Bolsista b JOIN b.projetos p WHERE p.publicId = :projPublicId AND b.usuario.ativo = true ORDER BY b.usuario.nome")
    List<Bolsista> findByProjetosPublicIdAndAtivoTrueOrderByNome(@Param("projPublicId") String projPublicId);

    @Query("SELECT COUNT(b) FROM Bolsista b JOIN b.projetos p WHERE p.id = :projetoId AND b.usuario.ativo = true")
    int countByProjetos_IdAndAtivoTrue(@Param("projetoId") UUID projetoId);

    @Query("SELECT COUNT(b) FROM Bolsista b WHERE b.laboratorioId = :laboratorioId AND b.usuario.ativo = true")
    int countByLaboratorioIdAndAtivoTrue(@Param("laboratorioId") UUID laboratorioId);

    @Query("SELECT COUNT(b) FROM Bolsista b WHERE b.usuario.tipoUsuario = :tipoUsuario AND b.usuario.ativo = true")
    int countByTipoUsuarioAndAtivoTrue(@Param("tipoUsuario") String tipoUsuario);
}
