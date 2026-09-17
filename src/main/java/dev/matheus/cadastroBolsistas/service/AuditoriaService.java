package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.model.Auditoria;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.AuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public List<Auditoria> buscarLogs(String entidade, String acao, LocalDateTime dataInicio, LocalDateTime dataFim, Integer limit, Integer offset) {
        Pageable pageable = Pageable.unpaged();
        if (limit != null && limit > 0 && offset != null && offset >= 0) {
            pageable = PageRequest.of(offset / limit, limit);
        }
        return new ArrayList<>(repository.buscarLogs(entidade, acao, dataInicio, dataFim, pageable));
    }

    public int contarLogs(String entidade, String acao, LocalDateTime dataInicio, LocalDateTime dataFim) {
        return repository.contarLogs(entidade, acao, dataInicio, dataFim);
    }
}
