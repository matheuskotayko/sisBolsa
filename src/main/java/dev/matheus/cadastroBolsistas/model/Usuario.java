package dev.matheus.cadastroBolsistas.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import java.util.UUID;

/*
 * campos comuns de qualquer usuario do sistema (ADMIN, BOLSISTA ou PROFESSOR).
 * chave primaria do tipo UUID.
 */
@MappedSuperclass
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    public Usuario() {}

    public Usuario(UUID id, String nome, String email, String senha, boolean ativo, String tipoUsuario, String fotoUrl, String nomeLaboratorio) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.ativo = ativo;
        this.tipoUsuario = tipoUsuario;
        this.fotoUrl = fotoUrl;
        this.nomeLaboratorio = nomeLaboratorio;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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
        return "ADMIN".equals(this.tipoUsuario);
    }

    public boolean isBolsista() {
        return "BOLSISTA".equals(this.tipoUsuario);
    }

    public boolean isProfessor() {
        return "PROFESSOR".equals(this.tipoUsuario);
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
