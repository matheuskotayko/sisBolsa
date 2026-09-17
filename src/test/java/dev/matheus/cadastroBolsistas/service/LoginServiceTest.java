package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.BolsistaRepository;
import dev.matheus.cadastroBolsistas.repository.ProfessorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final String SENHA = "12345678";

    @Mock
    private BolsistaRepository bolsistaRepository;

    @Mock
    private ProfessorRepository professorRepository;

    /* encoder de verdade: o ponto do teste e justamente a comparacao do bcrypt */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private LoginService loginService() {
        return new LoginService(bolsistaRepository, professorRepository, passwordEncoder);
    }

    private Bolsista bolsistaCom(String email, String senhaEmClaro) {
        Bolsista b = new Bolsista();
        b.setEmail(email);
        b.setSenha(passwordEncoder.encode(senhaEmClaro));
        return b;
    }

    @Test
    void autenticar_retornaBolsistaQuandoSenhaConfere() {
        String email = "bolsista@teste.com";
        when(bolsistaRepository.findByEmailAndAtivoTrue(email))
                .thenReturn(Optional.of(bolsistaCom(email, SENHA)));

        Usuario resultado = loginService().autenticar(email, SENHA);

        assertNotNull(resultado);
        assertEquals(email, resultado.getEmail());
        verifyNoInteractions(professorRepository);
    }

    @Test
    void autenticar_retornaNullQuandoSenhaNaoConfere() {
        String email = "bolsista@teste.com";
        when(bolsistaRepository.findByEmailAndAtivoTrue(email))
                .thenReturn(Optional.of(bolsistaCom(email, SENHA)));

        assertNull(loginService().autenticar(email, "senhaErrada"));
    }

    @Test
    void autenticar_caiParaProfessorQuandoNaoEhBolsista() {
        String email = "professor@teste.com";
        Professor p = new Professor();
        p.setEmail(email);
        p.setSenha(passwordEncoder.encode(SENHA));

        when(bolsistaRepository.findByEmailAndAtivoTrue(email)).thenReturn(Optional.empty());
        when(professorRepository.findByEmailAndAtivoTrue(email)).thenReturn(Optional.of(p));

        Usuario resultado = loginService().autenticar(email, SENHA);

        assertNotNull(resultado);
        assertEquals(email, resultado.getEmail());
    }

    @Test
    void autenticar_retornaNullQuandoEmailNaoExiste() {
        String email = "inexistente@teste.com";
        when(bolsistaRepository.findByEmailAndAtivoTrue(email)).thenReturn(Optional.empty());
        when(professorRepository.findByEmailAndAtivoTrue(email)).thenReturn(Optional.empty());

        assertNull(loginService().autenticar(email, SENHA));
    }

    @Test
    void autenticar_comEmailOuSenhaNull_retornaNullSemConsultarBanco() {
        assertNull(loginService().autenticar(null, SENHA));
        assertNull(loginService().autenticar("alguem@teste.com", null));

        verifyNoInteractions(bolsistaRepository, professorRepository);
    }

    @Test
    void senhaNuncaEhGuardadaEmTextoPuro() {
        String email = "bolsista@teste.com";
        Bolsista b = bolsistaCom(email, SENHA);

        assertNotEquals(SENHA, b.getSenha());
        assertTrue(b.getSenha().startsWith("$2a$"));
    }

    @Test
    void buscarPorEmail_procuraBolsistaAntesDeProfessor() {
        String email = "alguem@teste.com";
        when(bolsistaRepository.findByEmailAndAtivoTrue(email))
                .thenReturn(Optional.of(bolsistaCom(email, SENHA)));

        assertNotNull(loginService().buscarPorEmail(email));
        verifyNoInteractions(professorRepository);
    }
}
