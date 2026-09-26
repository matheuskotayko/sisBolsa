package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.BolsistaRequest;
import dev.matheus.cadastroBolsistas.dto.PerfilRequest;
import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Cargo;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.ModalidadeBolsa;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.BolsistaRepository;
import dev.matheus.cadastroBolsistas.repository.LaboratorioRepository;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/*
 * Regras de negócio de bolsistas com suporte a identificadores públicos e UUIDs.
 */
@Service
public class BolsistaService {

    private final BolsistaRepository repository;
    private final LaboratorioRepository laboratorioRepository;
    private final LaboratorioService laboratorioService;
    private final PasswordEncoder passwordEncoder;

    public BolsistaService(BolsistaRepository repository,
                           LaboratorioRepository laboratorioRepository,
                           LaboratorioService laboratorioService,
                           PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.laboratorioRepository = laboratorioRepository;
        this.laboratorioService = laboratorioService;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean podeGerenciar(Usuario usuarioLogado, Bolsista b) {
        if (usuarioLogado == null || b == null) return false;
        if (usuarioLogado.isAdmin()) return true;
        if (usuarioLogado.isProfessor()) {
            if (b.getLaboratorioId() != null) {
                Laboratorio lab = laboratorioRepository.findById(b.getLaboratorioId()).orElse(null);
                return lab != null && Objects.equals(lab.getCoordenadorId(), usuarioLogado.getId());
            }
        }
        return false;
    }

    public void exigirPodeCadastrarUsuario(Usuario logado) {
        if (logado.isBolsista()) {
            throw new PermissaoNegadaException("Bolsista nao cadastra usuario.");
        }
    }

    public void exigirPodeExportarUsuarios(Usuario logado) {
        if (logado.isBolsista()) {
            throw new PermissaoNegadaException("Bolsista nao exporta a lista de usuarios.");
        }
    }

    public void exigirPermissao(Usuario logado, Bolsista b, String acao) {
        if (!Objects.equals(logado.getId(), b.getId()) && !podeGerenciar(logado, b)) {
            throw new PermissaoNegadaException("Sem permissao para " + acao + " este usuario.");
        }
    }

    public boolean inserir(Bolsista b) {
        b.setAtivo(true);
        repository.save(b);
        return true;
    }

    public ArrayList<Bolsista> listarTodos() {
        return new ArrayList<>(repository.findByAtivoTrueOrderByNome());
    }

    public Bolsista buscarPorId(String publicId) {
        if (publicId == null || publicId.isBlank()) return null;
        Bolsista b = repository.findByPublicIdAndAtivoTrue(publicId).orElse(null);
        if (b == null) {
            try {
                UUID uuid = UUID.fromString(publicId);
                return repository.findById(uuid).filter(Bolsista::isAtivo).orElse(null);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return b;
    }

    public Bolsista buscarPorId(UUID id) {
        if (id == null) return null;
        return repository.findById(id).filter(Bolsista::isAtivo).orElse(null);
    }

    public Bolsista buscarOuFalhar(String publicId) {
        Bolsista b = buscarPorId(publicId);
        if (b == null) {
            throw new RecursoNaoEncontradoException("Usuario nao encontrado.");
        }
        return b;
    }

    public Bolsista buscarOuFalhar(UUID id) {
        Bolsista b = buscarPorId(id);
        if (b == null) {
            throw new RecursoNaoEncontradoException("Usuario nao encontrado.");
        }
        return b;
    }

    public Bolsista buscarComPermissaoDeVisualizacao(String publicId, Usuario logado) {
        Bolsista b = buscarOuFalhar(publicId);
        exigirPermissao(logado, b, "ver");
        return b;
    }

    public Bolsista buscarComPermissaoDeVisualizacao(UUID id, Usuario logado) {
        Bolsista b = buscarOuFalhar(id);
        exigirPermissao(logado, b, "ver");
        return b;
    }

    public Bolsista buscarComPermissaoDeEdicao(String publicId, Usuario logado) {
        Bolsista b = buscarOuFalhar(publicId);
        exigirPermissao(logado, b, "editar");
        return b;
    }

    public Bolsista buscarComPermissaoDeEdicao(UUID id, Usuario logado) {
        Bolsista b = buscarOuFalhar(id);
        exigirPermissao(logado, b, "editar");
        return b;
    }

    public Bolsista buscarComPermissaoDeExclusao(String publicId, Usuario logado) {
        Bolsista b = buscarOuFalhar(publicId);
        if (!podeGerenciar(logado, b)) {
            throw new PermissaoNegadaException("Sem permissao para excluir este usuario.");
        }
        return b;
    }

    public Bolsista buscarComPermissaoDeExclusao(UUID id, Usuario logado) {
        Bolsista b = buscarOuFalhar(id);
        if (!podeGerenciar(logado, b)) {
            throw new PermissaoNegadaException("Sem permissao para excluir este usuario.");
        }
        return b;
    }

    public ArrayList<Bolsista> buscarPorNome(String nome) {
        return new ArrayList<>(repository.findByNomeContainingIgnoreCaseAndAtivoTrueOrderByNome(nome));
    }

    public ArrayList<Bolsista> buscarPorCurso(String curso) {
        return new ArrayList<>(repository.findByCursoContainingIgnoreCaseAndAtivoTrueOrderByNome(curso));
    }

    public ArrayList<Bolsista> buscarPorLaboratorio(String labPublicId) {
        if (labPublicId == null || labPublicId.isBlank()) return new ArrayList<>();
        return new ArrayList<>(repository.findByLaboratorioPublicIdAndAtivoTrueOrderByNome(labPublicId));
    }

    public ArrayList<Bolsista> buscarPorLaboratorio(UUID laboratorioId) {
        if (laboratorioId == null) return new ArrayList<>();
        return new ArrayList<>(repository.findByLaboratorioIdAndAtivoTrueOrderByNome(laboratorioId));
    }

    public ArrayList<Bolsista> buscarPorProjeto(String projPublicId) {
        if (projPublicId == null || projPublicId.isBlank()) return new ArrayList<>();
        return new ArrayList<>(repository.findByProjetosPublicIdAndAtivoTrueOrderByNome(projPublicId));
    }

    public ArrayList<Bolsista> buscarPorProjeto(UUID projetoId) {
        if (projetoId == null) return new ArrayList<>();
        return new ArrayList<>(repository.findByProjetos_IdAndAtivoTrueOrderByNome(projetoId));
    }

    public boolean atualizar(Bolsista b) {
        repository.save(b);
        return true;
    }

    @Transactional
    public boolean excluir(String publicId) {
        if (publicId == null || publicId.isBlank()) return false;
        return repository.findByPublicId(publicId).map(b -> {
            b.setAtivo(false);
            repository.save(b);
            return true;
        }).orElseGet(() -> {
            try {
                UUID uuid = UUID.fromString(publicId);
                return excluir(uuid);
            } catch (IllegalArgumentException e) {
                return false;
            }
        });
    }

    @Transactional
    public boolean excluir(UUID id) {
        if (id == null) return false;
        return repository.findById(id).map(b -> {
            b.setAtivo(false);
            repository.save(b);
            return true;
        }).orElse(false);
    }

    public ArrayList<Bolsista> filtrarPorEscopo(ArrayList<Bolsista> lista, Usuario usuarioLogado) {
        if (usuarioLogado == null) {
            return new ArrayList<>();
        }
        if (usuarioLogado.isAdmin()) {
            return lista;
        }
        if (usuarioLogado.isProfessor()) {
            List<Laboratorio> labsCoordenados =
                    laboratorioRepository.findByCoordenadorIdAndAtivoTrueOrderByNome(usuarioLogado.getId());
            Set<UUID> labIds = labsCoordenados.stream()
                    .map(Laboratorio::getId)
                    .collect(Collectors.toSet());

            ArrayList<Bolsista> filtrados = new ArrayList<>();
            for (Bolsista b : lista) {
                if (b.getLaboratorioId() != null && labIds.contains(b.getLaboratorioId())) {
                    filtrados.add(b);
                }
            }
            return filtrados;
        }
        if (usuarioLogado.isBolsista()) {
            Bolsista bLogado = buscarPorId(usuarioLogado.getId());
            UUID labId = bLogado != null ? bLogado.getLaboratorioId() : null;
            ArrayList<Bolsista> filtrados = new ArrayList<>();
            for (Bolsista b : lista) {
                if (Objects.equals(b.getLaboratorioId(), labId)) {
                    filtrados.add(b);
                }
            }
            return filtrados;
        }
        return new ArrayList<>();
    }

    public void validarSenha(String senha, boolean exigirSenha) {
        if (exigirSenha && (StringUtil.estaVazio(senha) || senha.length() < 6)) {
            throw new IllegalArgumentException("Senha e obrigatoria e precisa ter ao menos 6 caracteres.");
        }
        if (!exigirSenha && !StringUtil.estaVazio(senha) && senha.length() < 6) {
            throw new IllegalArgumentException("A nova senha precisa ter ao menos 6 caracteres.");
        }
    }

    public void aplicarComuns(Bolsista b, BolsistaRequest body) {
        b.setNome(StringUtil.limpar(body.nome()));
        b.setEmail(StringUtil.limpar(body.email()));
        b.setFotoUrl(body.fotoUrl());
        b.setBio(body.bio());
        b.setAtivo(true);
    }

    public void aplicarCamposDeBolsista(Bolsista b, BolsistaRequest body, Usuario logado) {
        b.setDataNascimento(body.dataNascimento());
        b.setCurso(body.curso());
        b.setMatricula(body.matricula());
        b.setCpf(body.cpf());
        b.setTelefone(body.telefone());
        b.setCargo(Cargo.deString(body.cargo()));
        b.setModalidadeBolsa(ModalidadeBolsa.deString(body.modalidadeBolsa()));
        b.setValorBolsa(body.valorBolsa());
        b.setDataInicioBolsa(body.dataInicioBolsa());
        b.setDataFimBolsa(body.dataFimBolsa());
        b.setTipoUsuario("BOLSISTA");

        String labId = body.laboratorioId();
        if (labId != null && !labId.isBlank()) {
            Laboratorio lab = laboratorioRepository.findByPublicIdAndAtivoTrue(labId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Laboratorio nao encontrado com id: " + labId));
            if (!laboratorioService.podeGerenciar(logado, lab.getId())) {
                throw new PermissaoNegadaException("Sem permissao para vincular usuario a este laboratorio.");
            }
            b.setLaboratorio(lab);
        } else {
            b.setLaboratorio(null);
            b.setLaboratorioId(null);
        }
    }

    public void aplicarDadosPerfil(Usuario u, PerfilRequest body, String senhaNova) {
        u.setNome(StringUtil.limpar(body.nome()));
        u.setEmail(StringUtil.limpar(body.email()));
        u.setFotoUrl(body.fotoUrl());
        u.setBio(body.bio());
        if (senhaNova != null) {
            u.setSenha(senhaNova);
        }
    }

    public String calcularNovaSenha(Usuario logado, String senhaAtual, String senhaNova, String confirmaSenha) {
        boolean trocando = !StringUtil.estaVazio(senhaAtual) || !StringUtil.estaVazio(senhaNova) || !StringUtil.estaVazio(confirmaSenha);
        if (!trocando) {
            return null;
        }
        if (StringUtil.estaVazio(senhaAtual) || StringUtil.estaVazio(senhaNova) || StringUtil.estaVazio(confirmaSenha)) {
            throw new IllegalArgumentException("Para alterar a senha, preencha a senha atual, a nova e a confirmacao.");
        }
        if (!passwordEncoder.matches(senhaAtual, logado.getSenha())) {
            throw new IllegalArgumentException("A senha atual informada esta incorreta.");
        }
        if (senhaNova.length() < 6) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 6 caracteres.");
        }
        if (!senhaNova.equals(confirmaSenha)) {
            throw new IllegalArgumentException("A nova senha e a confirmacao nao coincidem.");
        }
        return passwordEncoder.encode(senhaNova);
    }

    public ArrayList<Bolsista> filtrarPorTipo(ArrayList<Bolsista> lista, String tipo) {
        if (StringUtil.estaVazio(tipo)) {
            return lista;
        }
        String filtro = tipo.trim().toUpperCase();
        lista.removeIf(u -> !filtro.equals(u.getTipoUsuario()));
        return lista;
    }

    public List<UUID> idsDosBolsistasCoordenadosPor(UUID professorId) {
        return laboratorioService.listarPorCoordenador(professorId).stream()
                .flatMap(lab -> buscarPorLaboratorio(lab.getId()).stream())
                .map(Bolsista::getId)
                .toList();
    }
}
