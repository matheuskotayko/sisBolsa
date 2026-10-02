package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Bolsista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BolsistaRepository extends JpaRepository<Bolsista, UUID> {

    Optional<Bolsista> findByPublicId(String publicId);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Bolsista b WHERE b.publicId = :publicId AND b.usuario.ativo = true")
    Optional<Bolsista> findByPublicIdAndAtivoTrue(@org.springframework.data.repository.query.Param("publicId") String publicId);

    Optional<Bolsista> findByEmailAndAtivoTrue(String email);

    List<Bolsista> findByAtivoTrueOrderByNome();

    List<Bolsista> findByNomeContainingIgnoreCaseAndAtivoTrueOrderByNome(String nome);

    List<Bolsista> findByCursoContainingIgnoreCaseAndAtivoTrueOrderByNome(String curso);

    List<Bolsista> findByLaboratorioIdAndAtivoTrueOrderByNome(UUID laboratorioId);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Bolsista b WHERE b.laboratorio.publicId = :labPublicId AND b.usuario.ativo = true ORDER BY b.usuario.nome")
    List<Bolsista> findByLaboratorioPublicIdAndAtivoTrueOrderByNome(@org.springframework.data.repository.query.Param("labPublicId") String labPublicId);

    /* navega pelo N:N mapeado em Bolsista.projetos - dispensa o JOIN nativo na bolsista_projeto. */
    List<Bolsista> findByProjetos_IdAndAtivoTrueOrderByNome(UUID projetoId);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Bolsista b JOIN b.projetos p WHERE p.publicId = :projPublicId AND b.usuario.ativo = true ORDER BY b.usuario.nome")
    List<Bolsista> findByProjetosPublicIdAndAtivoTrueOrderByNome(@org.springframework.data.repository.query.Param("projPublicId") String projPublicId);

    int countByProjetos_IdAndAtivoTrue(UUID projetoId);

    int countByLaboratorioIdAndAtivoTrue(UUID laboratorioId);

    int countByTipoUsuarioAndAtivoTrue(String tipoUsuario);
}
