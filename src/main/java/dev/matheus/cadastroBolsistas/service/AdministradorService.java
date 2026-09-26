package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.AdministradorRequest;
import dev.matheus.cadastroBolsistas.dto.AdministradorResponse;
import dev.matheus.cadastroBolsistas.exceptions.LimiteAdminsAtingidoException;
import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Administrador;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.AdministradorRepository;
import dev.matheus.cadastroBolsistas.repository.UsuarioRepository;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AdministradorService {

    public static final int LIMITE_ADMINS = 3;

    @Autowired
    private AdministradorRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public int contarAdmins() {
        return repository.countAtivos();
    }

    public boolean podeCriarAdmin() {
        return contarAdmins() < LIMITE_ADMINS;
    }

    public void exigirPodeCriarAdmin(Usuario logado) {
        if (logado == null || !logado.isAdmin()) {
            throw new PermissaoNegadaException("Requer perfil de administrador.");
        }
        if (!podeCriarAdmin()) {
            throw new LimiteAdminsAtingidoException("Limite de administradores atingido.");
        }
    }

    public void exigirVagaParaNovoAdmin() {
        if (!podeCriarAdmin()) {
            throw new LimiteAdminsAtingidoException("O sistema ja possui o numero maximo de administradores permitido.");
        }
    }

    public List<AdministradorResponse> listarTodos() {
        return repository.findByAtivoTrueOrderByNome()
                .stream()
                .map(AdministradorResponse::de)
                .toList();
    }

    public Administrador buscarPorId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new RecursoNaoEncontradoException("Administrador nao encontrado com id: " + publicId);
        }
        return repository.findByPublicIdAndAtivoTrue(publicId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Administrador nao encontrado com id: " + publicId));
    }

    public Administrador buscarPorId(UUID id) {
        return repository.findById(id)
                .filter(Administrador::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Administrador nao encontrado com id: " + id));
    }

    @Transactional
    public AdministradorResponse criar(AdministradorRequest body, Usuario logado) {
        exigirPodeCriarAdmin(logado);
        return AdministradorResponse.de(salvarNovo(body));
    }

    @Transactional
    public Administrador criarAdminAutocadastro(String nome, String email, String senhaHash) {
        exigirVagaParaNovoAdmin();
        if (usuarioRepository.existsByEmail(email)) {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint uk_usuario_email");
        }

        Usuario u = new Usuario();
        u.setNome(StringUtil.limpar(nome));
        u.setEmail(StringUtil.limpar(email));
        u.setSenha(senhaHash);
        u.setTipoUsuario("ADMIN");
        u.setAtivo(true);

        Administrador admin = new Administrador();
        admin.setUsuario(u);
        admin.setCargo("Administrador Geral");

        return repository.save(admin);
    }

    @Transactional
    public AdministradorResponse atualizar(String publicId, AdministradorRequest body, Usuario logado) {
        Administrador admin = buscarPorId(publicId);
        return executarAtualizacao(admin, body, logado);
    }

    @Transactional
    public AdministradorResponse atualizar(UUID id, AdministradorRequest body, Usuario logado) {
        Administrador admin = buscarPorId(id);
        return executarAtualizacao(admin, body, logado);
    }

    private AdministradorResponse executarAtualizacao(Administrador admin, AdministradorRequest body, Usuario logado) {
        if (logado == null || !logado.isAdmin()) {
            throw new PermissaoNegadaException("Requer perfil de administrador.");
        }

        String novoEmail = StringUtil.limpar(body.email());

        if (!Objects.equals(admin.getEmail(), novoEmail)) {
            usuarioRepository.findByEmail(novoEmail).ifPresent(outro -> {
                if (!outro.getId().equals(admin.getId())) {
                    throw new IllegalArgumentException("Ja existe outro usuario cadastrado com o e-mail: " + novoEmail);
                }
            });
            admin.setEmail(novoEmail);
        }

        admin.setNome(StringUtil.limpar(body.nome()));
        admin.setFotoUrl(body.fotoUrl());
        admin.setBio(body.bio());
        admin.setCargo(body.cargo());
        admin.setTelefone(body.telefone());

        if (!StringUtil.estaVazio(body.senha())) {
            if (body.senha().length() < 6) {
                throw new IllegalArgumentException("A nova senha precisa ter ao menos 6 caracteres.");
            }
            admin.setSenha(passwordEncoder.encode(body.senha()));
        }

        return AdministradorResponse.de(repository.save(admin));
    }

    @Transactional
    public void desativar(String publicId, Usuario logado) {
        Administrador admin = buscarPorId(publicId);
        executarDesativacao(admin, logado);
    }

    @Transactional
    public void desativar(UUID id, Usuario logado) {
        Administrador admin = buscarPorId(id);
        executarDesativacao(admin, logado);
    }

    private void executarDesativacao(Administrador admin, Usuario logado) {
        if (logado == null || !logado.isAdmin()) {
            throw new PermissaoNegadaException("Requer perfil de administrador.");
        }

        if (contarAdmins() <= 1) {
            throw new IllegalStateException("Nao e permitido desativar o unico administrador ativo do sistema.");
        }

        admin.setAtivo(false);
        repository.save(admin);
    }

    private Administrador salvarNovo(AdministradorRequest body) {
        String email = StringUtil.limpar(body.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint uk_usuario_email");
        }

        if (StringUtil.estaVazio(body.senha()) || body.senha().length() < 6) {
            throw new IllegalArgumentException("Senha e obrigatoria e precisa ter ao menos 6 caracteres.");
        }

        Usuario u = new Usuario();
        u.setNome(StringUtil.limpar(body.nome()));
        u.setEmail(email);
        u.setSenha(passwordEncoder.encode(body.senha()));
        u.setTipoUsuario("ADMIN");
        u.setAtivo(true);
        u.setFotoUrl(body.fotoUrl());
        u.setBio(body.bio());

        Administrador admin = new Administrador();
        admin.setUsuario(u);
        admin.setCargo(body.cargo() != null ? body.cargo() : "Administrador Geral");
        admin.setTelefone(body.telefone());

        return repository.save(admin);
    }
}
