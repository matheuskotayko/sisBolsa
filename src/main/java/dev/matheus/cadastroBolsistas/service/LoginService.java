package dev.matheus.cadastroBolsistas.service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginService(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario autenticar(String email, String senha) {
        if (email == null || senha == null) {
            return null;
        }
        Usuario usuario = buscarPorEmail(email);
        if (usuario == null) {
            return null;
        }
        return passwordEncoder.matches(senha, usuario.getSenha()) ? usuario : null;
    }

    /*
     * Procura diretamente na entidade Usuario, unificada para todos os perfis
     * (Administrador, Bolsista, Professor). Usado tambem pelo filtro JWT para
     * repor o usuario no contexto de seguranca.
     */
    public Usuario buscarPorEmail(String email) {
        if (email == null) {
            return null;
        }
        return usuarioRepository.findByEmailAndAtivoTrue(email).orElse(null);
    }

    public Usuario buscarPorPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return null;
        }
        return usuarioRepository.findByPublicIdAndAtivoTrue(publicId).orElse(null);
    }

    // Rate limiting from LoginService
public static final int MAX_TENTATIVAS = 5;
    public static final long DURACAO_BLOQUEIO_SEGUNDOS = 5 * 60; // 5 minutos

    private static class TentativaInfo {
        int tentativas;
        Instant expiracaoBloqueio;
        Instant ultimoErro;

        TentativaInfo() {
            this.tentativas = 1;
            this.ultimoErro = Instant.now();
        }
    }

    private final Map<String, TentativaInfo> cache = new ConcurrentHashMap<>();

    public boolean isBloqueado(String chave) {
        if (chave == null || chave.isBlank()) return false;
        chave = chave.toLowerCase().trim();
        TentativaInfo info = cache.get(chave);
        if (info == null) return false;

        if (info.expiracaoBloqueio != null) {
            if (Instant.now().isBefore(info.expiracaoBloqueio)) {
                return true;
            } else {
                /* bloqueio expirou, reseta o estado */
                cache.remove(chave);
                return false;
            }
        }
        return false;
    }

    public void registrarFalha(String chave) {
        if (chave == null || chave.isBlank()) return;
        chave = chave.toLowerCase().trim();

        cache.compute(chave, (k, info) -> {
            if (info == null) {
                return new TentativaInfo();
            }

            /* se ja passou mais de 15 minutos do ultimo erro sem bloquear, reinicia contador */
            if (info.ultimoErro != null && Instant.now().isAfter(info.ultimoErro.plusSeconds(15 * 60))) {
                info.tentativas = 1;
                info.ultimoErro = Instant.now();
                info.expiracaoBloqueio = null;
                return info;
            }

            info.tentativas++;
            info.ultimoErro = Instant.now();
            if (info.tentativas >= MAX_TENTATIVAS) {
                info.expiracaoBloqueio = Instant.now().plusSeconds(DURACAO_BLOQUEIO_SEGUNDOS);
            }
            return info;
        });
    }

    public void registrarSucesso(String chave) {
        if (chave == null || chave.isBlank()) return;
        cache.remove(chave.toLowerCase().trim());
    }

    public int getTentativasRestantes(String chave) {
        if (chave == null || chave.isBlank()) return MAX_TENTATIVAS;
        TentativaInfo info = cache.get(chave.toLowerCase().trim());
        if (info == null) return MAX_TENTATIVAS;
        return Math.max(0, MAX_TENTATIVAS - info.tentativas);
    }

    public long getSegundosRestantesBloqueio(String chave) {
        if (chave == null || chave.isBlank()) return 0;
        TentativaInfo info = cache.get(chave.toLowerCase().trim());
        if (info == null || info.expiracaoBloqueio == null) return 0;
        long segundos = info.expiracaoBloqueio.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, segundos);
    }
}
