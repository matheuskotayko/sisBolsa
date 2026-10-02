package dev.matheus.cadastroBolsistas.model;

import dev.matheus.cadastroBolsistas.util.PublicIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.UUID;

/*
 * Entidade concreta de usuario responsavel por identidade e credenciais (ADMIN, BOLSISTA ou PROFESSOR).
 * Chave primaria do tipo UUID e identificador publico seguro.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false, length = 36)
    private String publicId;

    private String nome;
    private String email;
    private String senha;
    private boolean ativo;

    @Column(name = "tipo_usuario")
    private String tipoUsuario; // 'ADMIN', 'BOLSISTA', 'PROFESSOR'

    @Column(name = "foto_url")
    private String fotoUrl;

    @Transient
    private String nomeLaboratorio;

    private String bio;

    public Usuario() {
        this.publicId = PublicIdGenerator.generateUsuarioId();
    }

    public Usuario(UUID id, String nome, String email, String senha, boolean ativo, String tipoUsuario, String fotoUrl, String nomeLaboratorio) {
        this.id = id;
        this.publicId = PublicIdGenerator.generateUsuarioId();
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.ativo = ativo;
        this.tipoUsuario = tipoUsuario;
        this.fotoUrl = fotoUrl;
        this.nomeLaboratorio = nomeLaboratorio;
    }

    @PrePersist
    public void prePersist() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = PublicIdGenerator.generateUsuarioId();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getNomeLaboratorio() { return nomeLaboratorio; }
    public void setNomeLaboratorio(String nomeLaboratorio) { this.nomeLaboratorio = nomeLaboratorio; }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.tipoUsuario);
    }

    public boolean isBolsista() {
        return "BOLSISTA".equalsIgnoreCase(this.tipoUsuario);
    }

    public boolean isProfessor() {
        return "PROFESSOR".equalsIgnoreCase(this.tipoUsuario);
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
