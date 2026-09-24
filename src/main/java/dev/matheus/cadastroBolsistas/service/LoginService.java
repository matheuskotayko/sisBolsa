package dev.matheus.cadastroBolsistas.service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.BolsistaRepository;
import dev.matheus.cadastroBolsistas.repository.ProfessorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final BolsistaRepository bolsistaRepository;
    private final ProfessorRepository professorRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginService(BolsistaRepository bolsistaRepository,
                        ProfessorRepository professorRepository,
                        PasswordEncoder passwordEncoder) {
        this.bolsistaRepository = bolsistaRepository;
        this.professorRepository = professorRepository;
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
     * procura primeiro na tabela bolsista, que guarda bolsista e admin, e so
     * depois cai para professor. usado tambem pelo filtro do jwt para repor o
     * usuario na sessao.
     */
    public Usuario buscarPorEmail(String email) {
        Usuario bolsista = bolsistaRepository.findByEmailAndAtivoTrue(email).orElse(null);
        if (bolsista != null) {
            return bolsista;
        }
        return professorRepository.findByEmailAndAtivoTrue(email).orElse(null);
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
