package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.request.PerfilRequest;
import dev.matheus.cadastroBolsistas.dto.response.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Administrador;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.common.StringUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** edicao do proprio perfil: mesma regra para os tres perfis, so muda quem persiste. */
@Service
public class PerfilService {

    private final AdministradorService administradorService;
    private final ProfessorService professorService;
    private final BolsistaService bolsistaService;
    private final PasswordEncoder passwordEncoder;

    public PerfilService(AdministradorService administradorService, ProfessorService professorService,
                         BolsistaService bolsistaService, PasswordEncoder passwordEncoder) {
        this.administradorService = administradorService;
        this.professorService = professorService;
        this.bolsistaService = bolsistaService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse atualizar(Usuario logado, PerfilRequest body) {
        String senhaNova = calcularNovaSenha(logado, body.senhaAtual(), body.senha());

        if (logado.isAdmin()) {
            Administrador a = administradorService.buscarPorId(logado.getId());
            aplicar(a.getUsuario(), body, senhaNova);
            administradorService.atualizar(a);
            return UsuarioResponse.from(a.getUsuario());
        }
        if (logado.isProfessor()) {
            Professor p = professorService.buscarOuFalhar(logado.getId());
            aplicar(p.getUsuario(), body, senhaNova);
            professorService.atualizar(p);
            return UsuarioResponse.from(p);
        }
        Bolsista b = bolsistaService.buscarOuFalhar(logado.getId());
        aplicar(b.getUsuario(), body, senhaNova);
        bolsistaService.atualizar(b);
        return UsuarioResponse.from(b);
    }

    private void aplicar(Usuario u, PerfilRequest body, String senhaNova) {
        u.setNome(StringUtil.limpar(body.nome()));
        u.setEmail(StringUtil.limpar(body.email()));
        u.setFotoUrl(body.fotoUrl());
        u.setBio(body.bio());
        if (senhaNova != null) {
            u.setSenha(senhaNova);
        }
    }

    /** tamanho e confirmacao ja validados no PerfilRequest; aqui so o que depende do banco. */
    private String calcularNovaSenha(Usuario logado, String senhaAtual, String senhaNova) {
        if (StringUtil.estaVazio(senhaNova)) {
            return null;
        }
        if (StringUtil.estaVazio(senhaAtual) || !passwordEncoder.matches(senhaAtual, logado.getSenha())) {
            throw new IllegalArgumentException("A senha atual informada esta incorreta.");
        }
        return passwordEncoder.encode(senhaNova);
    }
}
