package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.UsuarioRepository;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * servico de gerenciamento de codigos temporarios para redefinicao de senha.
 */
@Service
public class PasswordResetService {

    private static final long EXPIRACAO_MINUTOS = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PasswordResetService() {
        this(null, null);
    }

    private static class CodigoResetInfo {
        final String codigo;
        final Instant expiracao;

        CodigoResetInfo(String codigo, Instant expiracao) {
            this.codigo = codigo;
            this.expiracao = expiracao;
        }
    }

    private final Map<String, CodigoResetInfo> codigos = new ConcurrentHashMap<>();

    public String gerarCodigo(String email) {
        if (email == null || email.isBlank()) return null;
        String chave = email.toLowerCase().trim();

        // Gera codigo numerico de 6 digitos (100000 a 999999)
        int num = 100000 + RANDOM.nextInt(900000);
        String codigo = String.valueOf(num);

        codigos.put(chave, new CodigoResetInfo(codigo, Instant.now().plusSeconds(EXPIRACAO_MINUTOS * 60)));
        return codigo;
    }

    public boolean validarCodigo(String email, String codigo) {
        if (email == null || codigo == null) return false;
        String chave = email.toLowerCase().trim();
        CodigoResetInfo info = codigos.get(chave);

        if (info == null) return false;
        if (Instant.now().isAfter(info.expiracao)) {
            codigos.remove(chave);
            return false;
        }

        return info.codigo.equals(codigo.trim());
    }

    public void invalidarCodigo(String email) {
        if (email == null) return;
        codigos.remove(email.toLowerCase().trim());
    }

    @Transactional
    public void redefinirSenha(String email, String codigo, String novaSenha, String confirmaSenha) {
        String emailLimpo = StringUtil.limpar(email);
        String codigoLimpo = StringUtil.limpar(codigo);
        String senhaLimpa = StringUtil.limpar(novaSenha);
        String confirmaLimpa = StringUtil.limpar(confirmaSenha);

        if (!validarCodigo(emailLimpo, codigoLimpo)) {
            throw new IllegalArgumentException("Código de verificação inválido ou expirado.");
        }

        if (!senhaLimpa.equals(confirmaLimpa)) {
            throw new IllegalArgumentException("A nova senha e a confirmação não conferem.");
        }

        if (usuarioRepository == null || passwordEncoder == null) {
            throw new IllegalStateException("Dependências necessárias não configuradas no serviço.");
        }

        Usuario u = usuarioRepository.findByEmailAndAtivoTrue(emailLimpo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        u.setSenha(passwordEncoder.encode(senhaLimpa));
        usuarioRepository.save(u);

        invalidarCodigo(emailLimpo);
    }
}
