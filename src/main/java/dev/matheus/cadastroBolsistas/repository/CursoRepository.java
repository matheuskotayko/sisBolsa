package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CursoRepository extends JpaRepository<Curso, UUID> {

    Optional<Curso> findByPublicId(String publicId);

    Optional<Curso> findByPublicIdAndAtivoTrue(String publicId);

    List<Curso> findByAtivoTrueOrderByNome();

    boolean existsByNomeIgnoreCaseAndAtivoTrue(String nome);
}
