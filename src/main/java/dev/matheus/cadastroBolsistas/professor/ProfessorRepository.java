package dev.matheus.cadastroBolsistas.professor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfessorRepository extends JpaRepository<Professor, UUID>, JpaSpecificationExecutor<Professor> {

    Optional<Professor> findByPublicId(String publicId);

    @Query("SELECT p FROM Professor p WHERE p.publicId = :publicId AND p.usuario.ativo = true")
    Optional<Professor> findByPublicIdAndAtivoTrue(@Param("publicId") String publicId);

    @Query("SELECT p FROM Professor p WHERE p.usuario.email = :email AND p.usuario.ativo = true")
    Optional<Professor> findByEmailAndAtivoTrue(@Param("email") String email);

    @Query("SELECT p FROM Professor p WHERE p.usuario.ativo = true ORDER BY p.usuario.nome")
    List<Professor> findByAtivoTrueOrderByNome();

}
