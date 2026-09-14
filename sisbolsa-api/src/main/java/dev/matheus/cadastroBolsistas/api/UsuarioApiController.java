package dev.matheus.cadastroBolsistas.api;

import dev.matheus.cadastroBolsistas.dto.BolsistaRequest;
import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.PaginaResponse;
import dev.matheus.cadastroBolsistas.dto.ProjetoResponse;
import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Cargo;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.ModalidadeBolsa;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.AuditoriaService;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.LaboratorioService;
import dev.matheus.cadastroBolsistas.service.ProfessorService;
import dev.matheus.cadastroBolsistas.service.ProjetoService;
import dev.matheus.cadastroBolsistas.util.PaginacaoUtil;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Tag(name = "Bolsistas & Usuários", description = "Gestão de bolsistas, professores e administradores, incluindo vigência, modalidades e cargos.")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioApiController {

    private static final int TAMANHO_PADRAO = 10;
    private static final int TAMANHO_MAXIMO = 200;

    private final BolsistaService bolsistaService;
    private final ProfessorService professorService;
    private final LaboratorioService laboratorioService;
    private final ProjetoService projetoService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioLogado usuarioLogado;
    private final AuditoriaService auditoriaService;

    public UsuarioApiController(BolsistaService bolsistaService, ProfessorService professorService,
                                LaboratorioService laboratorioService, ProjetoService projetoService,
                                PasswordEncoder passwordEncoder, UsuarioLogado usuarioLogado,
                                AuditoriaService auditoriaService) {
        this.bolsistaService = bolsistaService;
        this.professorService = professorService;
        this.laboratorioService = laboratorioService;
        this.projetoService = projetoService;
        this.passwordEncoder = passwordEncoder;
        this.usuarioLogado = usuarioLogado;
        this.auditoriaService = auditoriaService;
    }

    @Operation(summary = "Listar usuários paginados", description = "Retorna a listagem de usuários de acordo com o escopo do usuário autenticado: ADMIN visualiza todos, PROFESSOR visualiza bolsistas dos laboratórios que coordena, e BOLSISTA visualiza seus colegas de laboratório.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de usuários"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(
            @Parameter(description = "Número da página", example = "1") @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Quantidade de itens por página", example = "10") @RequestParam(required = false) Integer tamanho,
            @Parameter(description = "Filtro por perfil (ADMIN, PROFESSOR, BOLSISTA)", example = "BOLSISTA") @RequestParam(required = false) String tipo,
            @Parameter(description = "Filtro de busca textual por nome", example = "Lucas") @RequestParam(required = false) String buscaNome,
            @Parameter(description = "Filtro de busca textual por curso", example = "Engenharia") @RequestParam(required = false) String buscaCurso) {
        Usuario logado = usuarioLogado.obrigatorio();

        ArrayList<Usuario> lista = new ArrayList<>();
        if (!StringUtil.estaVazio(buscaNome)) {
            lista.addAll(bolsistaService.buscarPorNome(buscaNome));
            if (logado.isAdmin()) {
                lista.addAll(professorService.buscarPorNome(buscaNome));
            }
        } else if (!StringUtil.estaVazio(buscaCurso)) {
            lista.addAll(bolsistaService.buscarPorCurso(buscaCurso));
        } else {
            lista.addAll(bolsistaService.listarTodos());
            if (logado.isAdmin()) {
                lista.addAll(professorService.listarTodos());
            }
        }

        if (!StringUtil.estaVazio(tipo)) {
            String filtro = tipo.trim().toUpperCase();
            lista.removeIf(u -> !filtro.equals(u.getTipoUsuario()));
        }

        preencherLabsDosProfessores(lista);
        lista = bolsistaService.filtrarPorEscopo(lista, logado);

        return paginar(lista, pagina, tamanho);
    }

    @Operation(summary = "Buscar usuário por ID", description = "Recupera as informações detalhadas de um bolsista ou professor pelo seu identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do usuário", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso não autorizado para o perfil do usuário", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public UsuarioResponse buscar(
            @Parameter(description = "ID do usuário (UUID)", required = true) @PathVariable UUID id,
            @Parameter(description = "Tipo de perfil", example = "BOLSISTA") @RequestParam(defaultValue = "BOLSISTA") String tipo) {
        Usuario logado = usuarioLogado.obrigatorio();

        if ("PROFESSOR".equalsIgnoreCase(tipo)) {
            usuarioLogado.exigirAdmin(logado);
            Professor p = professorService.buscarPorId(id);
            if (p == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor nao encontrado.");
            }
            return UsuarioResponse.de(p);
        }

        Bolsista b = bolsistaService.buscarPorId(id);
        if (b == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado.");
        }
        usuarioLogado.exigir(Objects.equals(logado.getId(), id) || bolsistaService.podeGerenciar(logado, b),
                "Sem permissao para ver este usuario.");
        return UsuarioResponse.de(b);
    }

    @Operation(summary = "Listar cargos disponíveis", description = "Retorna todos os cargos cadastrados para bolsistas nos laboratórios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de cargos")
    })
    @GetMapping("/cargos")
    public List<Map<String, String>> cargos() {
        return java.util.Arrays.stream(Cargo.values())
                .map(c -> Map.of("valor", c.name(), "descricao", c.getDescricao()))
                .toList();
    }

    @Operation(summary = "Listar modalidades de bolsa", description = "Retorna todas as modalidades de bolsa suportadas pelo sistema (PIBIC, PIBITI, Extensão, Monitoria, etc.).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de modalidades")
    })
    @GetMapping("/modalidades")
    public List<Map<String, String>> modalidades() {
        return java.util.Arrays.stream(ModalidadeBolsa.values())
                .map(m -> Map.of("valor", m.name(), "descricao", m.getDescricao()))
                .toList();
    }

    @Operation(summary = "Exportar usuários em CSV", description = "Exporta em arquivo CSV a lista de bolsistas e professores visíveis para o usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo CSV para download"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para bolsistas", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/exportar")
    public void exportar(HttpServletResponse response) throws java.io.IOException {
        Usuario logado = usuarioLogado.obrigatorio();
        usuarioLogado.exigir(!logado.isBolsista(), "Bolsista nao exporta a lista de usuarios.");

        ArrayList<Usuario> lista = new ArrayList<>(bolsistaService.listarTodos());
        if (logado.isAdmin()) {
            lista.addAll(professorService.listarTodos());
        }
        preencherLabsDosProfessores(lista);
        lista = bolsistaService.filtrarPorEscopo(lista, logado);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=usuarios.csv");
        try (java.io.PrintWriter writer = response.getWriter()) {
            writer.println("ID,Nome,Email,Tipo,Curso,Matricula,Cargo,Modalidade,Valor,DataInicio,DataFim,Laboratorio");
            for (Usuario u : lista) {
                UsuarioResponse r = UsuarioResponse.de(u);
                writer.println(String.join(",",
                        String.valueOf(r.id()), csv(r.nome()), csv(r.email()), csv(r.tipoUsuario()),
                        csv(r.curso()), csv(r.matricula()), csv(r.cargo()),
                        csv(r.modalidadeBolsaDescricao()), csv(r.valorBolsa() != null ? String.format("%.2f", r.valorBolsa()) : ""),
                        csv(r.dataInicioBolsa() != null ? r.dataInicioBolsa().toString() : ""),
                        csv(r.dataFimBolsa() != null ? r.dataFimBolsa().toString() : ""),
                        csv(r.nomeLaboratorio())));
            }
        }
    }

    private static String csv(String valor) {
        if (valor == null) {
            return "";
        }
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    @Operation(summary = "Listar projetos vinculados a um usuário", description = "Retorna todos os projetos de pesquisa dos quais o bolsista é membro ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de projetos vinculados"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/projetos")
    public List<ProjetoResponse> projetos(@Parameter(description = "ID do bolsista", required = true) @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        Bolsista b = bolsistaService.buscarPorId(id);
        if (b == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado.");
        }
        usuarioLogado.exigir(Objects.equals(logado.getId(), id) || bolsistaService.podeGerenciar(logado, b),
                "Sem permissao para ver os projetos deste usuario.");
        return projetoService.listarPorBolsista(id).stream().map(ProjetoResponse::de).toList();
    }

    @Operation(summary = "Cadastrar novo bolsista ou professor", description = "Cria um novo usuário no sistema. Administradores podem criar qualquer tipo; Professores podem criar bolsistas para os laboratórios que coordenam.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados cadastrais inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para cadastrar usuários", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do usuário a ser cadastrado", required = true)
                                                 @Valid @RequestBody BolsistaRequest body,
                                                 UriComponentsBuilder uriBuilder) {
        Usuario logado = usuarioLogado.obrigatorio();
        usuarioLogado.exigir(!logado.isBolsista(), "Bolsista nao cadastra usuario.");
        validarSenha(body, true);

        if ("PROFESSOR".equalsIgnoreCase(body.tipoUsuario())) {
            usuarioLogado.exigirAdmin(logado);
            Professor p = new Professor();
            aplicarComuns(p, body);
            p.setSenha(passwordEncoder.encode(body.senha()));
            p.setAtivo(true);
            professorService.inserir(p);
            auditoriaService.registrar(logado, "CRIAR_PROFESSOR", "USUARIO", "Professor '" + p.getNome() + "' (" + p.getEmail() + ") cadastrado.", null);
            URI uri = uriBuilder.replacePath("/api/usuarios/{id}").buildAndExpand(p.getId()).toUri();
            return ResponseEntity.created(uri).body(UsuarioResponse.de(p));
        }

        if ("ADMIN".equalsIgnoreCase(body.tipoUsuario())) {
            usuarioLogado.exigirAdmin(logado);
            if (bolsistaService.contarAdmins() >= 3) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Limite de 3 administradores atingido.");
            }
        }

        Bolsista b = new Bolsista();
        aplicarComuns(b, body);
        aplicarCamposDeBolsista(b, body, logado);
        b.setSenha(passwordEncoder.encode(body.senha()));
        bolsistaService.inserir(b);
        auditoriaService.registrar(logado, "CRIAR_USUARIO", "USUARIO", "Usuário '" + b.getNome() + "' (" + b.getTipoUsuario() + ") cadastrado.", null);
        URI uri = uriBuilder.replacePath("/api/usuarios/{id}").buildAndExpand(b.getId()).toUri();
        return ResponseEntity.created(uri).body(UsuarioResponse.de(b));
    }

    @Operation(summary = "Atualizar bolsista ou professor", description = "Atualiza os dados de um usuário existente. Se o campo de senha for enviado em branco, a senha atual é preservada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar este usuário", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@Parameter(description = "ID do usuário a atualizar", required = true) @PathVariable UUID id,
                                     @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do usuário", required = true)
                                     @Valid @RequestBody BolsistaRequest body) {
        Usuario logado = usuarioLogado.obrigatorio();
        validarSenha(body, false);

        if ("PROFESSOR".equalsIgnoreCase(body.tipoUsuario())) {
            usuarioLogado.exigirAdmin(logado);
            Professor p = professorService.buscarPorId(id);
            if (p == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor nao encontrado.");
            }
            aplicarComuns(p, body);
            if (!StringUtil.estaVazio(body.senha())) {
                p.setSenha(passwordEncoder.encode(body.senha()));
            }
            professorService.atualizar(p);
            auditoriaService.registrar(logado, "ATUALIZAR_PROFESSOR", "USUARIO", "Professor '" + p.getNome() + "' atualizado.", null);
            return UsuarioResponse.de(p);
        }

        Bolsista b = bolsistaService.buscarPorId(id);
        if (b == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado.");
        }
        usuarioLogado.exigir(Objects.equals(logado.getId(), id) || bolsistaService.podeGerenciar(logado, b),
                "Sem permissao para editar este usuario.");

        String senhaAtual = b.getSenha();
        aplicarComuns(b, body);
        aplicarCamposDeBolsista(b, body, logado);
        b.setSenha(StringUtil.estaVazio(body.senha()) ? senhaAtual : passwordEncoder.encode(body.senha()));
        bolsistaService.atualizar(b);
        auditoriaService.registrar(logado, "ATUALIZAR_USUARIO", "USUARIO", "Usuário '" + b.getNome() + "' atualizado.", null);
        return UsuarioResponse.de(b);
    }

    @Operation(summary = "Desativar usuário (Soft Delete)", description = "Desativa um usuário (bolsista ou professor), mantendo o histórico de frequência e projetos íntegros no banco de dados.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário desativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para desativar este usuário", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID do usuário a desativar", required = true) @PathVariable UUID id,
                                        @Parameter(description = "Tipo de usuário", example = "BOLSISTA") @RequestParam(defaultValue = "BOLSISTA") String tipo) {
        Usuario logado = usuarioLogado.obrigatorio();

        if ("PROFESSOR".equalsIgnoreCase(tipo)) {
            usuarioLogado.exigirAdmin(logado);
            Professor p = professorService.buscarPorId(id);
            if (p == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor nao encontrado.");
            }
            professorService.excluir(id);
            auditoriaService.registrar(logado, "EXCLUIR_PROFESSOR", "USUARIO", "Professor '" + p.getNome() + "' desativado.", null);
            return ResponseEntity.noContent().build();
        }

        Bolsista b = bolsistaService.buscarPorId(id);
        if (b == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado.");
        }
        usuarioLogado.exigir(bolsistaService.podeGerenciar(logado, b), "Sem permissao para excluir este usuario.");
        bolsistaService.excluir(id);
        auditoriaService.registrar(logado, "EXCLUIR_USUARIO", "USUARIO", "Usuário '" + b.getNome() + "' desativado.", null);
        return ResponseEntity.noContent().build();
    }

    /*
     * nome e email ja sao cobertos por bean validation no BolsistaRequest.
     * a senha fica de fora de la porque a regra depende do contexto: obrigatoria
     * na criacao, opcional na edicao (vazio = mantem a senha atual).
     */
    private void validarSenha(BolsistaRequest body, boolean exigirSenha) {
        if (exigirSenha && (StringUtil.estaVazio(body.senha()) || body.senha().length() < 6)) {
            throw new IllegalArgumentException("Senha e obrigatoria e precisa ter ao menos 6 caracteres.");
        }
        if (!exigirSenha && !StringUtil.estaVazio(body.senha()) && body.senha().length() < 6) {
            throw new IllegalArgumentException("A nova senha precisa ter ao menos 6 caracteres.");
        }
    }

    private void aplicarComuns(Usuario u, BolsistaRequest body) {
        u.setNome(StringUtil.limpar(body.nome()));
        u.setEmail(StringUtil.limpar(body.email()));
        u.setFotoUrl(body.fotoUrl());
        u.setBio(body.bio());
        u.setAtivo(true);
    }

    private void aplicarCamposDeBolsista(Bolsista b, BolsistaRequest body, Usuario logado) {
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
        if (labId != null) {
            usuarioLogado.exigir(laboratorioService.podeGerenciar(logado, labId),
                    "Sem permissao para vincular usuario a este laboratorio.");
        }
        b.setLaboratorioId(labId);
    }

    private void preencherLabsDosProfessores(List<Usuario> lista) {
        for (Usuario u : lista) {
            if (u.isProfessor()) {
                List<Laboratorio> labs = laboratorioService.listarPorCoordenador(u.getId());
                u.setNomeLaboratorio(labs.isEmpty()
                        ? "Nenhum"
                        : labs.stream().map(Laboratorio::getNome).reduce((a, b) -> a + ", " + b).orElse("Nenhum"));
            }
        }
    }

    private PaginaResponse<UsuarioResponse> paginar(List<Usuario> lista, int pagina, Integer tamanhoPedido) {
        return PaginacaoUtil.paginar(lista, pagina, tamanhoPedido, TAMANHO_PADRAO, TAMANHO_MAXIMO, UsuarioResponse::de);
    }
}
