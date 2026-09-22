package dev.matheus.cadastroBolsistas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/*
 * projeto vinculado a um laboratorio com id UUID e links de entregaveis.
 */
@Entity
@Table(name = "projeto")
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;
    private String descricao;

    @Column(name = "link_repositorio")
    private String linkRepositorio;

    @Column(name = "link_documentacao")
    private String linkDocumentacao;

    @Column(name = "laboratorio_id")
    private UUID laboratorioId;

    @ManyToOne
    @JoinColumn(name = "laboratorio_id", insertable = false, updatable = false)
    private Laboratorio laboratorio;

    private boolean ativo;

    /*
     * lado dono do relacionamento N:N com bolsista. a tabela bolsista_projeto so
     * tem as duas FKs, entao o @JoinTable da conta dela sem entidade intermediaria
     * e sem SQL nativo pra vincular/desvincular.
     */
    @ManyToMany
    @JoinTable(name = "bolsista_projeto",
            joinColumns = @JoinColumn(name = "projeto_id"),
            inverseJoinColumns = @JoinColumn(name = "bolsista_id"))
    private Set<Bolsista> bolsistas = new HashSet<>();

    public Projeto() {}

    public Projeto(UUID id, String nome, String descricao, UUID laboratorioId, String nomeLaboratorio, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.laboratorioId = laboratorioId;
        this.ativo = ativo;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getLinkRepositorio() { return linkRepositorio; }
    public void setLinkRepositorio(String linkRepositorio) { this.linkRepositorio = linkRepositorio; }

    public String getLinkDocumentacao() { return linkDocumentacao; }
    public void setLinkDocumentacao(String linkDocumentacao) { this.linkDocumentacao = linkDocumentacao; }

    public UUID getLaboratorioId() { return laboratorioId; }
    public void setLaboratorioId(UUID laboratorioId) { this.laboratorioId = laboratorioId; }

    public String getNomeLaboratorio() {
        return laboratorio != null ? laboratorio.getNome() : null;
    }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public Set<Bolsista> getBolsistas() { return bolsistas; }
}
