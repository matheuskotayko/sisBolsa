package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador, UUID> {

    Optional<Administrador> findByPublicId(String publicId);

    @Query("SELECT a FROM Administrador a WHERE (a.publicId = :publicId OR a.usuario.publicId = :publicId) AND a.usuario.ativo = true")
    Optional<Administrador> findByPublicIdAndAtivoTrue(@Param("publicId") String publicId);

    @Query("SELECT a FROM Administrador a WHERE a.usuario.email = :email AND a.usuario.ativo = true")
    Optional<Administrador> findByEmailAndAtivoTrue(@Param("email") String email);

    @Query("SELECT a FROM Administrador a WHERE a.usuario.ativo = true ORDER BY a.usuario.nome")
    List<Administrador> findByAtivoTrueOrderByNome();

    @Query("SELECT COUNT(a) FROM Administrador a WHERE a.usuario.ativo = true")
    int countAtivos();
}
