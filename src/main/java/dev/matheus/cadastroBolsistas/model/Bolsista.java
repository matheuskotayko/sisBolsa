package dev.matheus.cadastroBolsistas.model;

import dev.matheus.cadastroBolsistas.util.PublicIdGenerator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/*
 * Entidade de perfil para bolsistas com id UUID, dados de vigencia e modalidade de bolsa.
 * Utiliza o padrao de composicao 1:1 com Usuario (chave primaria compartilhada via @MapsId)
 * e identificador publico prefixado bol_.
 */
@Entity
@Table(name = "bolsista")
public class Bolsista {

    @Id
    private UUID id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false, length = 36)
    private String publicId;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @MapsId
    @JoinColumn(name = "id")
    private Usuario usuario;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    private String curso;
    private String matricula;
    private String cpf;
    private String telefone;

    @Column(name = "laboratorio_id")
    private UUID laboratorioId;

    @ManyToOne
    @JoinColumn(name = "laboratorio_id", insertable = false, updatable = false)
    private Laboratorio laboratorio;

    /* lado inverso do N:N: navega de bolsista para projeto; quem grava e o Projeto. */
    @ManyToMany(mappedBy = "bolsistas")
    private Set<Projeto> projetos = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Cargo cargo;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade_bolsa")
    private ModalidadeBolsa modalidadeBolsa;

    @Column(name = "valor_bolsa")
    private Double valorBolsa;

    @Column(name = "data_inicio_bolsa")
    private LocalDate dataInicioBolsa;

    @Column(name = "data_fim_bolsa")
    private LocalDate dataFimBolsa;

    public Bolsista() {
        this.publicId = PublicIdGenerator.generateBolsistaId();
        this.usuario = new Usuario();
        this.usuario.setTipoUsuario("BOLSISTA");
        this.usuario.setAtivo(true);
    }

    public Bolsista(UUID id, String nome, String senha, LocalDate dataNascimento, String curso, String email,
                    String matricula, String cpf, String telefone, boolean ativo, UUID laboratorioId,
                    String nomeLaboratorio, String tipoUsuario, String fotoUrl, Cargo cargo) {
        this.id = id;
        this.publicId = PublicIdGenerator.generateBolsistaId();
        this.usuario = new Usuario(id, nome, email, senha, ativo, tipoUsuario != null ? tipoUsuario : "BOLSISTA", fotoUrl, nomeLaboratorio);
        this.dataNascimento = dataNascimento;
        this.curso = curso;
        this.matricula = matricula;
        this.cpf = cpf;
        this.telefone = telefone;
        this.laboratorioId = laboratorioId;
        this.cargo = cargo;
    }

    public Bolsista(UUID id, Usuario usuario) {
        this.id = id;
        this.publicId = PublicIdGenerator.generateBolsistaId();
        this.usuario = usuario;
        if (this.usuario != null && this.usuario.getTipoUsuario() == null) {
            this.usuario.setTipoUsuario("BOLSISTA");
        }
    }

    @PrePersist
    public void prePersist() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = PublicIdGenerator.generateBolsistaId();
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

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public UUID getLaboratorioId() { return laboratorioId; }
    public void setLaboratorioId(UUID laboratorioId) { this.laboratorioId = laboratorioId; }

    public Laboratorio getLaboratorio() { return laboratorio; }
    public void setLaboratorio(Laboratorio laboratorio) {
        this.laboratorio = laboratorio;
        if (laboratorio != null) {
            this.laboratorioId = laboratorio.getId();
        }
    }

    public String getLaboratorioPublicId() {
        return laboratorio != null ? laboratorio.getPublicId() : null;
    }

    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }

    public ModalidadeBolsa getModalidadeBolsa() { return modalidadeBolsa; }
    public void setModalidadeBolsa(ModalidadeBolsa modalidadeBolsa) { this.modalidadeBolsa = modalidadeBolsa; }

    public Double getValorBolsa() { return valorBolsa; }
    public void setValorBolsa(Double valorBolsa) { this.valorBolsa = valorBolsa; }

    public LocalDate getDataInicioBolsa() { return dataInicioBolsa; }
    public void setDataInicioBolsa(LocalDate dataInicioBolsa) { this.dataInicioBolsa = dataInicioBolsa; }

    public LocalDate getDataFimBolsa() { return dataFimBolsa; }
    public void setDataFimBolsa(LocalDate dataFimBolsa) { this.dataFimBolsa = dataFimBolsa; }

    public Set<Projeto> getProjetos() { return projetos; }

    public boolean isBolsaVencida() {
        return dataFimBolsa != null && dataFimBolsa.isBefore(LocalDate.now());
    }

    public boolean isBolsaPrestesAVencer() {
        if (dataFimBolsa == null) return false;
        LocalDate hoje = LocalDate.now();
        long dias = ChronoUnit.DAYS.between(hoje, dataFimBolsa);
        return dias >= 0 && dias <= 30;
    }

    // Metodos delegados para a identidade Usuario
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

    public String getTipoUsuario() { return usuario != null ? usuario.getTipoUsuario() : "BOLSISTA"; }
    public void setTipoUsuario(String tipoUsuario) {
        garantirUsuario();
        usuario.setTipoUsuario(tipoUsuario);
    }

    public String getNomeLaboratorio() {
        return laboratorio != null ? laboratorio.getNome() : (usuario != null ? usuario.getNomeLaboratorio() : null);
    }

    public void setNomeLaboratorio(String nomeLaboratorio) {
        garantirUsuario();
        usuario.setNomeLaboratorio(nomeLaboratorio);
    }

    public boolean isAdmin() {
        return usuario != null && usuario.isAdmin();
    }

    public boolean isBolsista() {
        return usuario != null && usuario.isBolsista();
    }

    public boolean isProfessor() {
        return usuario != null && usuario.isProfessor();
    }

    private void garantirUsuario() {
        if (this.usuario == null) {
            this.usuario = new Usuario();
            this.usuario.setTipoUsuario("BOLSISTA");
            this.usuario.setAtivo(true);
            if (this.id != null) {
                this.usuario.setId(this.id);
            }
        }
    }
}
