package dev.matheus.cadastroBolsistas.model;

import dev.matheus.cadastroBolsistas.util.PublicIdGenerator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

/*
 * Entidade de perfil para professores coordenadores e orientadores.
 * Utiliza o padrao de composicao 1:1 com Usuario (chave primaria compartilhada via @MapsId)
 * e identificador publico com prefixo prf_.
 */
@Entity
@Table(name = "professor")
public class Professor {

    @Id
    private UUID id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false, length = 36)
    private String publicId;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @MapsId
    @JoinColumn(name = "id")
    private Usuario usuario;

    public Professor() {
        this.publicId = PublicIdGenerator.generateProfessorId();
        this.usuario = new Usuario();
        this.usuario.setTipoUsuario("PROFESSOR");
        this.usuario.setAtivo(true);
    }

    public Professor(UUID id, String nome, String email, String senha, boolean ativo, String fotoUrl) {
        this.id = id;
        this.publicId = PublicIdGenerator.generateProfessorId();
        this.usuario = new Usuario(id, nome, email, senha, ativo, "PROFESSOR", fotoUrl, null);
    }

    public Professor(UUID id, Usuario usuario) {
        this.id = id;
        this.publicId = PublicIdGenerator.generateProfessorId();
        this.usuario = usuario;
        if (this.usuario != null) {
            this.usuario.setTipoUsuario("PROFESSOR");
        }
    }

    @PrePersist
    public void prePersist() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = PublicIdGenerator.generateProfessorId();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) {
        this.id = id;
        if (this.usuario != null) {
            this.usuario.setId(id);
        }
    }

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    // Metodos delegados para facilitar acesso aos dados de identidade
    public String getNome() { return usuario != null ? usuario.getNome() : null; }
    public void setNome(String nome) {
        garantirUsuario();
        usuario.setNome(nome);
    }

    public String getEmail() { return usuario != null ? usuario.getEmail() : null; }
    public void setEmail(String email) {
        garantirUsuario();
        usuario.setEmail(email);
    }

    public String getSenha() { return usuario != null ? usuario.getSenha() : null; }
    public void setSenha(String senha) {
        garantirUsuario();
        usuario.setSenha(senha);
    }

    public boolean isAtivo() { return usuario != null && usuario.isAtivo(); }
    public void setAtivo(boolean ativo) {
        garantirUsuario();
        usuario.setAtivo(ativo);
    }

    public String getFotoUrl() { return usuario != null ? usuario.getFotoUrl() : null; }
    public void setFotoUrl(String fotoUrl) {
        garantirUsuario();
        usuario.setFotoUrl(fotoUrl);
    }

    public String getBio() { return usuario != null ? usuario.getBio() : null; }
    public void setBio(String bio) {
        garantirUsuario();
        usuario.setBio(bio);
    }

    public String getTipoUsuario() { return "PROFESSOR"; }
    public boolean isAdmin() { return false; }
    public boolean isProfessor() { return true; }
    public boolean isBolsista() { return false; }

    public String getNomeLaboratorio() { return usuario != null ? usuario.getNomeLaboratorio() : null; }
    public void setNomeLaboratorio(String nomeLaboratorio) {
        garantirUsuario();
        usuario.setNomeLaboratorio(nomeLaboratorio);
    }

    private void garantirUsuario() {
        if (this.usuario == null) {
            this.usuario = new Usuario();
            this.usuario.setTipoUsuario("PROFESSOR");
            this.usuario.setAtivo(true);
            if (this.id != null) {
                this.usuario.setId(this.id);
            }
        }
    }
}
