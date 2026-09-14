import { api } from './api';
import type { Projeto, ProjetoRequest, MembroProjeto, Paginacao } from '../types';

export const projetoService = {
  listar: (filtros?: { buscaNome?: string; labId?: string; pagina?: number; tamanho?: number }) =>
    api.get<Paginacao<Projeto>>('/projetos', filtros),

  buscarPorId: (id: string) =>
    api.get<Projeto>(`/projetos/${id}`),

  criar: (dados: ProjetoRequest) =>
    api.post<Projeto>('/projetos', dados),

  atualizar: (id: string, dados: ProjetoRequest) =>
    api.put<Projeto>(`/projetos/${id}`, dados),

  excluir: (id: string) =>
    api.delete<void>(`/projetos/${id}`),

  listarMembros: (id: string) =>
    api.get<MembroProjeto[]>(`/projetos/${id}/membros`),

  vincularBolsista: (projetoId: string, bolsistaId: string) =>
    api.post<void>(`/projetos/${projetoId}/membros/${bolsistaId}`),

  desvincularBolsista: (projetoId: string, bolsistaId: string) =>
    api.delete<void>(`/projetos/${projetoId}/membros/${bolsistaId}`),
};
