package dev.matheus.cadastroBolsistas.professor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfessorRepository extends JpaRepository<Professor, UUID>, JpaSpecificationExecutor<Professor> {

    Optional<Professor> findByPublicId(String publicId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Professor p WHERE p.publicId = :publicId AND p.usuario.ativo = true")
    Optional<Professor> findByPublicIdAndAtivoTrue(@org.springframework.data.repository.query.Param("publicId") String publicId);

    Optional<Professor> findByEmailAndAtivoTrue(String email);

    List<Professor> findByAtivoTrueOrderByNome();
}
