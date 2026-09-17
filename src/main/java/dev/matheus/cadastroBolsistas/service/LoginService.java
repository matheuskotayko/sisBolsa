package dev.matheus.cadastroBolsistas.service;

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
}
