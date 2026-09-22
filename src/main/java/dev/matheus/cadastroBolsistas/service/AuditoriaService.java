package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.model.Auditoria;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.AuditoriaRepository;
import dev.matheus.cadastroBolsistas.repository.Filtros;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(UUID usuarioId, String usuarioNome, String acao, String entidade, String detalhes, String ip) {
        try {
            Auditoria a = new Auditoria(usuarioId, usuarioNome, acao, entidade, detalhes, ip);
            repository.save(a);
        } catch (Exception e) {
            /* auditoria nunca deve quebrar o fluxo principal da aplicacao */
        }
    }

    public void registrar(Usuario usuario, String acao, String entidade, String detalhes, String ip) {
        if (usuario != null) {
            registrar(usuario.getId(), usuario.getNome(), acao, entidade, detalhes, ip);
        } else {
            registrar(null, "Anônimo / Sistema", acao, entidade, detalhes, ip);
        }
    }

    private static final Sort MAIS_RECENTES = Sort.by(Sort.Direction.DESC, "dataHora");

    public List<Auditoria> buscarLogs(String entidade, String acao, LocalDateTime dataInicio, LocalDateTime dataFim, Integer limit, Integer offset) {
        Pageable pageable = Pageable.unpaged(MAIS_RECENTES);
        if (limit != null && limit > 0 && offset != null && offset >= 0) {
            pageable = PageRequest.of(offset / limit, limit, MAIS_RECENTES);
        }
        Specification<Auditoria> filtro = Filtros.auditoria(entidade, acao, dataInicio, dataFim);
        return new ArrayList<>(repository.findAll(filtro, pageable).getContent());
    }

    public int contarLogs(String entidade, String acao, LocalDateTime dataInicio, LocalDateTime dataFim) {
        return (int) repository.count(Filtros.auditoria(entidade, acao, dataInicio, dataFim));
    }

    public void exigirAcesso(Usuario logado) {
        if (!logado.isAdmin() && !logado.isProfessor()) {
            throw new PermissaoNegadaException("Acesso restrito a administradores e professores.");
        }
    }
}
