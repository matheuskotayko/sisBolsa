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
 * Entidade de perfil para administradores do sistema.
 * Utiliza o padrao de composicao 1:1 com Usuario (chave primaria compartilhada via @MapsId)
 * e identificador publico com prefixo adm_.
 */
@Entity
@Table(name = "administrador")
public class Administrador {

    @Id
    private UUID id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false, length = 36)
    private String publicId;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @MapsId
    @JoinColumn(name = "id")
    private Usuario usuario;

    private String cargo;
    private String telefone;

    public Administrador() {
        this.publicId = PublicIdGenerator.generateAdministradorId();
        this.usuario = new Usuario();
        this.usuario.setTipoUsuario("ADMIN");
        this.usuario.setAtivo(true);
    }

    public Administrador(UUID id, Usuario usuario, String cargo, String telefone) {
        this.id = id;
        this.publicId = PublicIdGenerator.generateAdministradorId();
        this.usuario = usuario;
        if (this.usuario != null) {
            this.usuario.setTipoUsuario("ADMIN");
        }
        this.cargo = cargo;
        this.telefone = telefone;
    }

    @PrePersist
    public void prePersist() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = PublicIdGenerator.generateAdministradorId();
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

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

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

    public String getTipoUsuario() { return "ADMIN"; }
    public boolean isAdmin() { return true; }
    public boolean isProfessor() { return false; }
    public boolean isBolsista() { return false; }

    private void garantirUsuario() {
        if (this.usuario == null) {
            this.usuario = new Usuario();
            this.usuario.setTipoUsuario("ADMIN");
            this.usuario.setAtivo(true);
            if (this.id != null) {
                this.usuario.setId(this.id);
            }
        }
    }
}
