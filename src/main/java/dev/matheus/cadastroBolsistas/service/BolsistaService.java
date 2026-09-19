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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/*
 * regras de negocio de bolsistas com IDs em UUID.
 */
@Service
public class BolsistaService {

    private static final int LIMITE_ADMINS = 3;

    @Autowired
    private BolsistaRepository repository;

    @Autowired
    private LaboratorioRepository laboratorioRepository;

    @Autowired
    private LaboratorioService laboratorioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    public boolean inserir(Bolsista b) {
        b.setAtivo(true);
        repository.save(b);
        return true;
    }

    public ArrayList<Bolsista> listarTodos() {
        return new ArrayList<>(repository.findByAtivoTrueOrderByNome());
    }

    public Bolsista buscarPorId(UUID id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    /*
     * PoC de mover regra de acesso pro service: lookup + 404 + permissao de
     * visualizacao (dono do proprio cadastro ou quem gerencia) num so lugar,
     * pra controller so chamar e montar a resposta.
     */
    public Bolsista buscarComPermissaoDeVisualizacao(UUID id, Usuario logado) {
        Bolsista b = buscarPorId(id);
        if (b == null) {
            throw new RecursoNaoEncontradoException("Usuario nao encontrado.");
        }
        if (!Objects.equals(logado.getId(), id) && !podeGerenciar(logado, b)) {
            throw new PermissaoNegadaException("Sem permissao para ver este usuario.");
        }
        return b;
    }

    public ArrayList<Bolsista> buscarPorNome(String nome) {
        return new ArrayList<>(repository.findByNomeContainingIgnoreCaseAndAtivoTrueOrderByNome(nome));
    }

    public ArrayList<Bolsista> buscarPorCurso(String curso) {
        return new ArrayList<>(repository.findByCursoContainingIgnoreCaseAndAtivoTrueOrderByNome(curso));
    }

    public ArrayList<Bolsista> buscarPorLaboratorio(UUID laboratorioId) {
        if (laboratorioId == null) return new ArrayList<>();
        return new ArrayList<>(repository.buscarPorLaboratorio(laboratorioId));
    }

    public ArrayList<Bolsista> buscarPorProjeto(UUID projetoId) {
        if (projetoId == null) return new ArrayList<>();
        return new ArrayList<>(repository.buscarPorProjeto(projetoId));
    }

    public boolean atualizar(Bolsista b) {
        repository.save(b);
        return true;
    }

    /* soft delete: marca ativo = false, nunca apaga a linha */
    @Transactional
    public boolean excluir(UUID id) {
        if (id == null) return false;
        return repository.desativar(id) > 0;
    }

    public ArrayList<Usuario> filtrarPorEscopo(ArrayList<Usuario> lista, Usuario usuarioLogado) {
        if (usuarioLogado == null) {
            return new ArrayList<>();
        }
        if (usuarioLogado.isAdmin()) {
            return lista;
        }
        if (usuarioLogado.isProfessor()) {
            ArrayList<Laboratorio> labsCoordenados =
                    new ArrayList<>(laboratorioRepository.buscarPorCoordenador(usuarioLogado.getId()));
            return somenteBolsistas(lista, b ->
                    labsCoordenados.stream().anyMatch(l -> Objects.equals(l.getId(), b.getLaboratorioId())));
        }
        if (usuarioLogado.isBolsista()) {
            UUID labId = ((Bolsista) usuarioLogado).getLaboratorioId();
            return somenteBolsistas(lista, b -> Objects.equals(b.getLaboratorioId(), labId));
        }
        return new ArrayList<>();
    }

    private ArrayList<Usuario> somenteBolsistas(ArrayList<Usuario> lista, java.util.function.Predicate<Bolsista> filtro) {
        ArrayList<Usuario> filtrados = new ArrayList<>();
        for (Usuario u : lista) {
            if (u instanceof Bolsista b && filtro.test(b)) {
                filtrados.add(b);
            }
        }
        return filtrados;
    }

    public int contarAdmins() {
        return repository.countByTipoUsuarioAndAtivoTrue("ADMIN");
    }

    public boolean podeCriarAdmin() {
        return contarAdmins() < LIMITE_ADMINS;
    }

    /* admin de autocadastro publico (AuthApiController) - dados cadastrais minimos sao fixos */
    public Bolsista criarAdmin(String nome, String email, String senhaHash) {
        Bolsista admin = new Bolsista();
        admin.setNome(nome);
        admin.setEmail(email);
        admin.setSenha(senhaHash);
        admin.setTipoUsuario("ADMIN");
        admin.setAtivo(true);
        admin.setDataNascimento(LocalDate.of(1990, 1, 1));
        admin.setCurso("Gestao");
        admin.setMatricula("ADM001");
        inserir(admin);
        return admin;
    }

    /*
     * nome e email ja sao cobertos por bean validation no BolsistaRequest.
     * a senha fica de fora de la porque a regra depende do contexto: obrigatoria
     * na criacao, opcional na edicao (vazio = mantem a senha atual).
     */
    public void validarSenha(String senha, boolean exigirSenha) {
        if (exigirSenha && (StringUtil.estaVazio(senha) || senha.length() < 6)) {
            throw new IllegalArgumentException("Senha e obrigatoria e precisa ter ao menos 6 caracteres.");
        }
        if (!exigirSenha && !StringUtil.estaVazio(senha) && senha.length() < 6) {
            throw new IllegalArgumentException("A nova senha precisa ter ao menos 6 caracteres.");
        }
    }

    public void aplicarComuns(Usuario u, BolsistaRequest body) {
        u.setNome(StringUtil.limpar(body.nome()));
        u.setEmail(StringUtil.limpar(body.email()));
        u.setFotoUrl(body.fotoUrl());
        u.setBio(body.bio());
        u.setAtivo(true);
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
        b.setTipoUsuario("ADMIN".equalsIgnoreCase(body.tipoUsuario()) ? "ADMIN" : "BOLSISTA");

        UUID labId = body.laboratorioId();
        if (labId != null && !laboratorioService.podeGerenciar(logado, labId)) {
            throw new PermissaoNegadaException("Sem permissao para vincular usuario a este laboratorio.");
        }
        b.setLaboratorioId(labId);
    }

    /* dados comuns de perfil (Bolsista ou Professor alterando o proprio cadastro) */
    public void aplicarDadosPerfil(Usuario u, PerfilRequest body, String senhaNova) {
        u.setNome(StringUtil.limpar(body.nome()));
        u.setEmail(StringUtil.limpar(body.email()));
        u.setFotoUrl(body.fotoUrl());
        u.setBio(body.bio());
        if (senhaNova != null) {
            u.setSenha(senhaNova);
        }
    }

    /* retorna a senha ja codificada, ou null se o usuario nao pediu troca de senha */
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

    public ArrayList<Usuario> filtrarPorTipo(ArrayList<Usuario> lista, String tipo) {
        if (StringUtil.estaVazio(tipo)) {
            return lista;
        }
        String filtro = tipo.trim().toUpperCase();
        lista.removeIf(u -> !filtro.equals(u.getTipoUsuario()));
        return lista;
    }

    /* laboratorios coordenados pelo professor -> ids de todos os bolsistas neles */
    public List<UUID> idsDosBolsistasCoordenadosPor(UUID professorId) {
        return laboratorioService.listarPorCoordenador(professorId).stream()
                .flatMap(lab -> buscarPorLaboratorio(lab.getId()).stream())
                .map(Usuario::getId)
                .toList();
    }
}
